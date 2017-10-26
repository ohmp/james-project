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
import org.apache.james.backends.es.IndexName;
import org.apache.james.mailbox.elasticsearch.tasks.IndexCreationConfiguration;
import org.apache.james.mailbox.elasticsearch.tasks.IndexWithMappingCreation;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.IndexCreationParameter;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.Service;

@Api(tags = "ElasticSearch")
@Path(IndexCreationRoutes.ELASTICSEARCH_INDEX_ENDPOINT)
@Produces("application/json")
public class IndexCreationRoutes implements Routes {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexCreationRoutes.class);

    public static final String INDEX_NAME = "name";
    public static final String ELASTICSEARCH_INDEX_ENDPOINT = "/elasticsearch/index";
    private final IndexWithMappingCreation indexWithMappingCreation;
    private final JsonExtractor<IndexCreationParameter> jsonExtractor;

    @Inject
    public IndexCreationRoutes(IndexWithMappingCreation indexWithMappingCreation) {
        this.indexWithMappingCreation = indexWithMappingCreation;
        this.jsonExtractor = new JsonExtractor<>(IndexCreationParameter.class);
    }

    @Override
    public void define(Service service) {
        definePutIndex(service);
    }

    @PUT
    @ApiOperation(value = "Allow creating a new Index with the mapping of the appropriate version.")
    @ApiImplicitParams({
        @ApiImplicitParam(required = true, dataType = "string", name = "indexName", paramType = "path"),
        @ApiImplicitParam(required = true, dataType = "org.apache.james.webadmin.dto.IndexCreationParameter", paramType = "body")
    })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "OK. The index have been created."),
        @ApiResponse(code = 400, message = "The body is not a valid payload."),
        @ApiResponse(code = 500, message = "Internal server error - Something went bad on the server side.")
    })
    private void definePutIndex(Service service) {
        service.put(ELASTICSEARCH_INDEX_ENDPOINT + "/:" + INDEX_NAME, (req, res) -> {
            try {
                IndexCreationParameter creationParameter = jsonExtractor.parse(req.body());
                indexWithMappingCreation.createIndexWithMapping(
                    IndexCreationConfiguration.builder()
                        .indexName(new IndexName(req.params(INDEX_NAME)))
                        .nbReplica(creationParameter.getNbReplica())
                        .nbShards(creationParameter.getNbShards())
                        .schemaVersion(creationParameter.getSchemaVersion())
                        .addAliases(creationParameter.getAliases()
                            .stream()
                            .map(AliasName::new)
                            .collect(Guavate.toImmutableList()))
                        .build());
                res.status(204);
                return Constants.EMPTY_BODY;
            } catch (JsonExtractException e) {
                LOGGER.info("Malformed JSON", e);
                throw service.halt(400);
            }
        });
    }
}
