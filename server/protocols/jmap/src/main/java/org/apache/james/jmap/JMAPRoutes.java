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

package org.apache.james.jmap;

import static org.apache.james.jmap.HttpConstants.SC_BAD_REQUEST;
import static org.apache.james.jmap.HttpConstants.SC_INTERNAL_SERVER_ERROR;
import static org.apache.james.jmap.HttpConstants.SC_UNAUTHORIZED;

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

public interface JMAPRoutes {
    HttpServerRoutes define(HttpServerRoutes builder);

    BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> CORS_CONTROL = (req, res) -> res.header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
        .header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept")
        .send();

    Logger logger();

    default Mono<Void> handleInternalError(HttpServerResponse response, Throwable e) {
        logger().error("Internal error", e);
        return response.status(SC_INTERNAL_SERVER_ERROR).send();
    }

    default Mono<Void> handleBadRequest(HttpServerResponse response, Exception e) {
        logger().warn("Invalid request received.", e);
        return response.status(SC_BAD_REQUEST).send();
    }

    default Mono<Void> handleAuthenticationFailure(HttpServerResponse response, Exception e) {
        logger().warn("Unauthorized", e);
        return response.status(SC_UNAUTHORIZED).send();
    }
}
