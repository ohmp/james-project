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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.backends.es.AliasName;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageIndexer;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageIndexerProvider;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.IndexationParameter;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.Service;

@Api(tags = "ElasticSearch")
@Path(IndexationRoutes.ELASTICSEARCH_INDEXATION_ENDPOINT)
@Produces("application/json")
public class IndexationRoutes implements Routes {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexationRoutes.class);

    public static final String ELASTICSEARCH_INDEXATION_ENDPOINT = "/elasticsearch/indexation";
    private static final String ALIAS_NAME = "name";
    private static final String WEB_ADMIN_USER = "webAdmin";

    private final MailboxManager mailboxManager;
    private final ElasticSearchListeningMessageIndexerProvider indexerProvider;
    private final JsonExtractor<IndexationParameter> jsonExtractor;

    @Inject
    public IndexationRoutes(MailboxManager mailboxManager, ElasticSearchListeningMessageIndexerProvider indexerProvider) {
        this.mailboxManager = mailboxManager;
        this.indexerProvider = indexerProvider;
        this.jsonExtractor = new JsonExtractor<>(IndexationParameter.class);
    }

    @Override
    public void define(Service service) {
        definePutIndexation(service);
    }

    @PUT
    @ApiOperation(value = "Allow Indexing James events to an additional alias.")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = "aliasName", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "org.apache.james.webadmin.dto.IndexationParameter", paramType = "body")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "OK. The index have been created."),
        @ApiResponse(code = 400, message = "The body is not a valid payload."),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    private void definePutIndexation(Service service) {
        service.put(ELASTICSEARCH_INDEXATION_ENDPOINT + "/:" + ALIAS_NAME, (req, res) -> {
            try {
                IndexationParameter indexationParameter = jsonExtractor.parse(req.body());
                startIndexing(indexationParameter, new AliasName(req.params(ALIAS_NAME)));
                res.status(204);
                return Constants.EMPTY_BODY;
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                throw service.halt(400);
            }
        });
    }

    private void startIndexing(IndexationParameter indexationParameter, AliasName aliasName) throws MailboxException {
        ElasticSearchListeningMessageIndexer messageIndexer = indexerProvider.provide(
            aliasName, indexationParameter.getSchemaVersion());
        MailboxSession mailboxSession = mailboxManager.createSystemSession(WEB_ADMIN_USER);
        mailboxManager.addGlobalListener(messageIndexer, mailboxSession);
        LOGGER.info("{} added to global mailbox listeners");
    }
}
