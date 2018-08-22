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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.james.core.healthcheck.HealthCheck;
import org.apache.james.core.healthcheck.Result;
import org.apache.james.webadmin.Routes;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.Service;

@Api(tags = "Healthchecks")
@Path(HealthCheckRoutes.HEALTHCHECK)
public class HealthCheckRoutes implements Routes {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRoutes.class);

    public static final String HEALTHCHECK = "/healthcheck";


    private final Set<HealthCheck> healthChecks;
    private Service service;

    @Inject
    public HealthCheckRoutes(Set<HealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Override
    public void define(Service service) {
        this.service = service;

        validateHealthchecks();
    }

    @GET
    @ApiOperation(value = "Validate all health checks")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.OK_200, message = "OK"),
            @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500,
                message = "Internal server error - When one check has failed.")
    })
    public void validateHealthchecks() {
        service.get(HEALTHCHECK,
            (request, response) -> {
                List<Result> anyUnhealthy = retrieveUnhealthyHealthChecks();

                if (!anyUnhealthy.isEmpty()) {
                    anyUnhealthy.stream()
                        .forEach(result -> LOGGER.error("HealthCheck failed " + result.getCause().orElse("")));

                    response.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                } else {
                    response.status(HttpStatus.OK_200);
                }
                return response;
            });
    }

    private List<Result> retrieveUnhealthyHealthChecks() {
        return healthChecks.stream()
            .map(HealthCheck::check)
            .filter(check -> !check.isHealthy())
            .collect(Guavate.toImmutableList());
    }
}
