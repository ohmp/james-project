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

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.james.lifecycle.api.Startable;
import org.apache.james.util.Port;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class JMAPServer implements Startable {
    private static final int RANDOM_PORT = 0;

    private final JMAPConfiguration configuration;
    private final Set<JMAPRoutes> jmapRoutes;
    private Optional<DisposableServer> server;

    @Inject
    public JMAPServer(JMAPConfiguration configuration, Set<JMAPRoutes> jmapRoutes) {
        this.configuration = configuration;
        this.jmapRoutes = jmapRoutes;
        this.server = Optional.empty();
    }

    public Port getPort() {
        return server.map(DisposableServer::port)
            .map(Port::of)
            .orElseThrow(() -> new IllegalStateException("port is not available because server is not started or disabled"));
    }

    public void start() {
        if (configuration.isEnabled()) {
            ImmutableListMultimap<JMAPRoutes.Endpoint, JMAPRoutes.JmapRoute> collect = jmapRoutes.stream()
                .flatMap(JMAPRoutes::routes)
                .collect(Guavate.toImmutableListMultimap(JMAPRoutes.JmapRoute::getEndpoint));

            server = Optional.of(HttpServer.create()
                .port(configuration.getPort()
                    .map(Port::getValue)
                    .orElse(RANDOM_PORT))
                .route(routes -> collect.asMap().forEach(
                    ((endpoint, r) -> {
                        if (r.size() == 1) {
                            JMAPRoutes.JmapRoute next = r.iterator().next();
                            switch (endpoint.getVerb()) {
                                case GET:
                                    routes.get(endpoint.getPath(), (req, res) -> {
                                        switch (next.getVersion()) {
                                            case DRAFT:
                                                // todo draft precondition
                                            case RFC8621:
                                                // todo rfc-8621 precondition
                                        }
                                        return next.getAction().apply(req, res);
                                    });
                                 // todo other verbs
                            }
                        } else if (r.size() == 2) {
                            ImmutableList<JMAPRoutes.JmapRoute> sorted = r.stream()
                                .sorted(Comparator.comparing(JMAPRoutes.JmapRoute::getVersion))
                                .collect(Guavate.toImmutableList());
                            JMAPRoutes.JmapRoute draftRoute = sorted.get(0);
                            JMAPRoutes.JmapRoute rfc8621Route = sorted.get(1);

                            switch (endpoint.getVerb()) {
                                case GET:
                                    if (hasDraftAcceptHeader()) {
                                        return draftRoute.getAction().apply(req, res);
                                    }
                                    if (hasRfc8621AcceptHeader()) {
                                        return rfc8621Route.getAction().apply(req, res);
                                    }
                                    // todo otherwize 400
                            }
                        }

                    })
                ))
                .wiretap(configuration.wiretapEnabled())
                .bindNow());
        }
    }

    @PreDestroy
    public void stop() {
        server.ifPresent(DisposableServer::disposeNow);
    }
}
