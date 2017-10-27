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
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.tasks.ElasticSearchReIndexer;
import org.apache.james.mailbox.elasticsearch.tasks.ElasticSearchReIndexerProvider;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.ReIndexationParameter;
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
import spark.Response;
import spark.Service;

@Api(tags = "ElasticSearch")
@Path(ReIndexationRoutes.ELASTICSEARCH_RE_INDEXATION_ENDPOINT)
@Produces("application/json")
public class ReIndexationRoutes implements Routes {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReIndexationRoutes.class);

    public static final String ELASTICSEARCH_RE_INDEXATION_ENDPOINT = "/elasticsearch/reIndexation";
    private static final String ALIAS_NAME = "name";
    public static final String MAILBOX_ID = "mailboxId";
    public static final String UID = "uid";

    private final ElasticSearchReIndexerProvider reIndexerProvider;
    private final JsonExtractor<ReIndexationParameter> jsonExtractor;
    private final MailboxId.Factory mailboxIdFactory;

    @Inject
    public ReIndexationRoutes(ElasticSearchReIndexerProvider reIndexerProvider,
                              MailboxId.Factory mailboxIdFactory) {
        this.reIndexerProvider = reIndexerProvider;
        this.mailboxIdFactory = mailboxIdFactory;
        this.jsonExtractor = new JsonExtractor<>(ReIndexationParameter.class);
    }

    @Override
    public void define(Service service) {
        definePutReIndexation(service);
        definePutMailboxReIndexation(service);
        definePutMessageReIndexation(service);
    }

    @PUT
    @ApiOperation(value = "Allow Re-Indexing all emails from all mailboxes")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = "aliasName", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "org.apache.james.webadmin.dto.ReIndexationParameter", paramType = "body")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "OK. The index have been created."),
        @ApiResponse(code = 400, message = "The body is not a valid payload."),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    private void definePutReIndexation(Service service) {
        service.put(ELASTICSEARCH_RE_INDEXATION_ENDPOINT + "/:" + ALIAS_NAME, (req, res) -> {
            try {
                ReIndexationParameter reIndexationParameter = jsonExtractor.parse(req.body());
                ElasticSearchReIndexer elasticSearchReIndexer = reIndexerProvider.provideReindexer(
                    new AliasName(req.params(ALIAS_NAME)), reIndexationParameter.getSchemaVersion());
                ElasticSearchReIndexer.ReIndexingResult result = elasticSearchReIndexer.reindexAll();
                return handleResult(result, service, res);
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                throw service.halt(400);
            }
        });
    }

    @PUT
    @ApiOperation(value = "Allow Re-Indexing all emails from a mailbox")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = "aliasName", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "string", name = "mailboxId", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "org.apache.james.webadmin.dto.ReIndexationParameter", paramType = "body")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "OK. The index have been created."),
        @ApiResponse(code = 400, message = "The body is not a valid payload."),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    private void definePutMailboxReIndexation(Service service) {
        service.put(ELASTICSEARCH_RE_INDEXATION_ENDPOINT +
                    "/:" + ALIAS_NAME +
                    "/:" + MAILBOX_ID, (req, res) -> {
            try {
                ReIndexationParameter reIndexationParameter = jsonExtractor.parse(req.body());
                MailboxId mailboxId = mailboxIdFactory.fromString(req.params(MAILBOX_ID));
                ElasticSearchReIndexer elasticSearchReIndexer = reIndexerProvider.provideReindexer(
                    new AliasName(req.params(ALIAS_NAME)), reIndexationParameter.getSchemaVersion());
                ElasticSearchReIndexer.ReIndexingResult result = elasticSearchReIndexer.reIndex(mailboxId);
                return handleResult(result, service, res);
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                throw service.halt(400);
            }
        });
    }

    @PUT
    @ApiOperation(value = "Allow Re-Indexing a specific message.")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = "aliasName", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "string", name = "mailboxId", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "string", name = "uid", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "org.apache.james.webadmin.dto.ReIndexationParameter", paramType = "body")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "OK. The index have been created."),
        @ApiResponse(code = 400, message = "The body is not a valid payload."),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    private void definePutMessageReIndexation(Service service) {
        service.put(ELASTICSEARCH_RE_INDEXATION_ENDPOINT +
                    "/:" + ALIAS_NAME +
                    "/:" + MAILBOX_ID +
                    "/:" + UID, (req, res) -> {
            try {
                ReIndexationParameter reIndexationParameter = jsonExtractor.parse(req.body());
                MailboxId mailboxId = mailboxIdFactory.fromString(req.params(MAILBOX_ID));
                MessageUid uid = MessageUid.of(Long.valueOf(req.params(UID)));
                ElasticSearchReIndexer elasticSearchReIndexer = reIndexerProvider.provideReindexer(
                    new AliasName(req.params(ALIAS_NAME)), reIndexationParameter.getSchemaVersion());
                ElasticSearchReIndexer.ReIndexingResult result = elasticSearchReIndexer.reIndex(mailboxId, uid);
                return handleResult(result, service, res);
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                throw service.halt(400);
            }
        });
    }

    private String handleResult(ElasticSearchReIndexer.ReIndexingResult result, Service service, Response res) {
        switch (result) {
            case SUCCESS:
                res.status(204);
                return Constants.EMPTY_BODY;
            case FAILED:
                throw service.halt(400);
            default:
                throw new IllegalArgumentException("Unexpected " + result + " token");
        }
    }
}
