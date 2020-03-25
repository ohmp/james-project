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
package org.apache.james.jmap.http;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.james.jmap.HttpConstants.JSON_CONTENT_TYPE;
import static org.apache.james.jmap.http.JMAPUrls.JMAP;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.james.jmap.JMAPRoutes;
import org.apache.james.jmap.draft.exceptions.BadRequestException;
import org.apache.james.jmap.draft.exceptions.InternalErrorException;
import org.apache.james.jmap.draft.exceptions.UnauthorizedException;
import org.apache.james.jmap.draft.methods.RequestHandler;
import org.apache.james.jmap.draft.model.AuthenticatedRequest;
import org.apache.james.jmap.draft.model.InvocationRequest;
import org.apache.james.jmap.draft.model.InvocationResponse;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.metrics.api.MetricFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

public class JMAPApiRoutes implements JMAPRoutes {
    public static final Logger LOGGER = LoggerFactory.getLogger(JMAPApiRoutes.class);

    private final ObjectMapper objectMapper;
    private final RequestHandler requestHandler;
    private final MetricFactory metricFactory;
    private final Authenticator authenticator;
    private final UserProvisioner userProvisioner;
    private final DefaultMailboxesProvisioner defaultMailboxesProvisioner;

    @Inject
    public JMAPApiRoutes(RequestHandler requestHandler, MetricFactory metricFactory, Authenticator authenticator, UserProvisioner userProvisioner, DefaultMailboxesProvisioner defaultMailboxesProvisioner) {
        this.requestHandler = requestHandler;
        this.metricFactory = metricFactory;
        this.authenticator = authenticator;
        this.userProvisioner = userProvisioner;
        this.defaultMailboxesProvisioner = defaultMailboxesProvisioner;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public HttpServerRoutes define(HttpServerRoutes builder) {
        return builder.post(JMAP, JMAPRoutes.corsHeaders(this::post))
            .options(JMAP, CORS_CONTROL);
    }

    private Mono<Void> post(HttpServerRequest request, HttpServerResponse response) {
        return authenticator.authenticate(request)
            .flatMap(session -> Flux.merge(
                    userProvisioner.provisionUser(session),
                    defaultMailboxesProvisioner.createMailboxesIfNeeded(session))
                .then()
                .thenReturn(session))
            .flatMap(session -> Mono.from(metricFactory.runPublishingTimerMetric("JMAP-request",
                post(request, response, session))))
            .onErrorResume(BadRequestException.class, e -> handleBadRequest(response, e))
            .onErrorResume(UnauthorizedException.class, e -> handleAuthenticationFailure(response, e))
            .onErrorResume(e -> handleInternalError(response, e))
            .subscribeOn(Schedulers.elastic());
    }

    private Mono<Void> post(HttpServerRequest request, HttpServerResponse response, MailboxSession session) {
        Flux<Object[]> responses =
            requestAsJsonStream(request)
                .map(InvocationRequest::deserialize)
                .map(invocationRequest -> AuthenticatedRequest.decorate(invocationRequest, session))
                .concatMap(this::handle)
                .map(InvocationResponse::asProtocolSpecification);

        return sendResponses(response, responses);
    }

    private Mono<Void> sendResponses(HttpServerResponse response, Flux<Object[]> responses) {
        return responses.collectList()
            .map(objects -> {
                try {
                    return objectMapper.writeValueAsString(objects);
                } catch (JsonProcessingException e) {
                    throw new InternalErrorException("error serialising JMAP API response json");
                }
            })
            .flatMap(json -> response.status(OK)
                .header(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .sendString(Mono.just(json))
                .then());
    }

    private Flux<? extends InvocationResponse> handle(AuthenticatedRequest request) {
        return Mono.fromCallable(() -> requestHandler.handle(request))
            .flatMapMany(Flux::fromStream)
            .subscribeOn(Schedulers.elastic());
    }

    private Flux<JsonNode[]> requestAsJsonStream(HttpServerRequest req) {
        return req.receive().aggregate().asInputStream()
            .map(inputStream -> {
                try {
                    return objectMapper.readValue(inputStream, JsonNode[][].class);
                } catch (IOException e) {
                    throw new BadRequestException("Error deserializing JSON", e);
                }
            })
            .flatMapMany(Flux::fromArray);
    }
}
