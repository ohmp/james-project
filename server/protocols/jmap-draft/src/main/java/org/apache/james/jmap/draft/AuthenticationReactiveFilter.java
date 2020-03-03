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
package org.apache.james.jmap.draft;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.jmap.draft.exceptions.UnauthorizedException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.metrics.api.MetricFactory;

import com.google.common.annotations.VisibleForTesting;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

public class AuthenticationReactiveFilter {
    private final List<ReactiveAuthenticationStrategy> authMethods;
    private final MetricFactory metricFactory;

    @Inject
    @VisibleForTesting
    AuthenticationReactiveFilter(List<ReactiveAuthenticationStrategy> authMethods, MetricFactory metricFactory) {
        this.authMethods = authMethods;
        this.metricFactory = metricFactory;
    }

    public Mono<MailboxSession> authenticate(HttpServerRequest request) {
        return metricFactory.runPublishingTimerMetric("JMAP-authentication-filter",
            Flux.fromStream(authMethods.stream())
                .flatMap(auth -> createSession(auth, request))
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new UnauthorizedException())));
    }

    private Mono<MailboxSession> createSession(ReactiveAuthenticationStrategy authenticationMethod, HttpServerRequest httpRequest) {
        try {
            return authenticationMethod.createMailboxSession(httpRequest);
        } catch (Exception e) {
            return Mono.empty();
        }
    }
}
