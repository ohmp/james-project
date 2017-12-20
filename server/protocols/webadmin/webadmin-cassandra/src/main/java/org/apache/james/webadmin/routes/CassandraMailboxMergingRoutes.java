/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.webadmin.routes;

import javax.inject.Inject;

import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.mail.task.MailboxMergingTask;
import org.apache.james.mailbox.cassandra.mail.task.MailboxMergingTaskRunner;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.MailboxMergingRequest;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.ErrorResponder.ErrorType;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;
import spark.Service;

public class CassandraMailboxMergingRoutes implements Routes {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMailboxMergingRoutes.class);

    public static final String BASE = "/cassandra/mailbox/merging";

    private final MailboxMergingTaskRunner mailboxMergingTaskRunner;
    private final CassandraId.Factory mailboxIdFactory;
    private final JsonExtractor<MailboxMergingRequest> jsonExtractor;

    public static String PARTIAL_MERGING_PROCESS = "An error lead to partial merging process.Check server logs";

    @Inject
    public CassandraMailboxMergingRoutes(MailboxMergingTaskRunner mailboxMergingTaskRunner, CassandraId.Factory mailboxIdFactory) {
        this.mailboxMergingTaskRunner = mailboxMergingTaskRunner;
        this.mailboxIdFactory = mailboxIdFactory;
        this.jsonExtractor = new JsonExtractor<>(MailboxMergingRequest.class);
    }

    @Override
    public void define(Service service) {
        service.post(BASE, this::mergeMailboxes);
    }

    private Object mergeMailboxes(Request request, Response response) {
        try {
            LOGGER.debug("Cassandra upgrade launched");
            MailboxMergingRequest mailboxMergingRequest = jsonExtractor.parse(request.body());
            CassandraId originId = mailboxIdFactory.fromString(mailboxMergingRequest.getMergeOrigin());
            CassandraId destinationId = mailboxIdFactory.fromString(mailboxMergingRequest.getMergeDestination());

            new MailboxMergingTask(mailboxMergingTaskRunner, originId, destinationId)
                .run()
                .ifCompleted(() -> response.status(HttpStatus.NO_CONTENT_204))
                .ifPartial(() -> {
                    throw internalError().haltError();
                });

            return Constants.EMPTY_BODY;
        } catch (JsonExtractException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorType.INVALID_ARGUMENT)
                .cause(e)
                .message("Failed to parse JSON request")
                .haltError();
        } catch (MailboxException e) {
            throw internalError()
                .cause(e)
                .haltError();
        }
    }

    private ErrorResponder internalError() {
        return ErrorResponder.builder()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
            .type(ErrorType.SERVER_ERROR)
            .message(PARTIAL_MERGING_PROCESS);
    }
}
