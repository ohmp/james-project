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

package org.apache.james.mock.smtp.server;

import java.io.IOException;

import org.apache.james.spark.Constants;
import org.apache.james.spark.PortSupplier;
import org.apache.james.spark.utils.JsonTransformer;
import org.apache.james.spark.utils.Responses;
import org.apache.james.util.Port;
import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;

import spark.Request;
import spark.Response;
import spark.Service;

public class HTTPConfigurationServer {
    private final Service service;
    private final JsonTransformer jsonTransformer;
    private final ObjectMapper objectMapper;
    private final SMTPBehaviorRepository smtpBehaviorRepository;
    private final PortSupplier portSupplier;

    public HTTPConfigurationServer(SMTPBehaviorRepository smtpBehaviorRepository, PortSupplier portSupplier) {
        this.smtpBehaviorRepository = smtpBehaviorRepository;
        this.portSupplier = portSupplier;
        this.service = Service.ignite();
        this.jsonTransformer = new JsonTransformer();
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule());
    }

    public void start() {
        service.port(portSupplier.get().getValue());

        service.before((request, response) -> response.type(Constants.JSON_CONTENT_TYPE));

        service.put("/smtpBehaviors", this::putBehaviors);
        service.get("/smtpBehaviors", this::getBehaviors, jsonTransformer);
        service.delete("/smtpBehaviors", this::deleteBehaviors);

        service.awaitInitialization();
    }

    public Port getPort() {
        return new Port(service.port());
    }

    public void stop() {
        service.stop();
        service.awaitStop();
    }

    String putBehaviors(Request request, Response response) {
        try {
            MockSmtpBehaviors behaviors = objectMapper.readValue(request.bodyAsBytes(), MockSmtpBehaviors.class);
            smtpBehaviorRepository.setBehaviors(behaviors);
        } catch (IOException e) {
            service.halt(HttpStatus.BAD_REQUEST_400, e.getMessage());
            Responses.returnNoContent(response);
        }
        return Responses.returnNoContent(response);
    }

    MockSmtpBehaviors getBehaviors(Request request, Response response) {
        response.status(HttpStatus.OK_200);
        return smtpBehaviorRepository.getBehaviors().orElse(new MockSmtpBehaviors(ImmutableList.of()));
    }

    String deleteBehaviors(Request request, Response response) {
        response.status(HttpStatus.NO_CONTENT_204);
        smtpBehaviorRepository.clearBehaviors();
        return Responses.returnNoContent(response);
    }
}
