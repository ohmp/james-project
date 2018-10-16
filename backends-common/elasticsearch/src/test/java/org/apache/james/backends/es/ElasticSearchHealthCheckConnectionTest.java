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
package org.apache.james.backends.es;

import java.util.HashSet;
import java.util.Set;

import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.RateLimiters;
import org.apache.james.util.docker.SwarmGenericContainer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticSearchHealthCheckConnectionTest {

    private static final int ES_APPLICATIVE_PORT = 9300;
    private static final Set<IndexName> indices = new HashSet<>();

    private static final WaitStrategy WAIT_STRATEGY = Wait.forHttp("/").forPort(ES_APPLICATIVE_PORT).withRateLimiter(RateLimiters.DEFAULT);

    @Rule
    public SwarmGenericContainer elasticSearchContainer = new SwarmGenericContainer(Images.ELASTICSEARCH)
        .withAffinityToContainer().withExposedPorts(ES_APPLICATIVE_PORT).waitingFor(WAIT_STRATEGY);

    @Test
    public void testHealthCheckValidation() {
        ClientProviderImpl clientProvider = ClientProviderImpl.forHost(elasticSearchContainer.getContainerIp(), ES_APPLICATIVE_PORT);
        Client client = clientProvider.get();
        indices.add(new IndexName("healthcheck"));
        ElasticSearchHealthCheck elasticSearchHealthCheck = new ElasticSearchHealthCheck(client, indices, 1000);

        String content = "{\"message\": \"trying out Elasticsearch Healthcheck\"}";

        IndexRequest testHealthCheckIndexRequest = new IndexRequest("healthcheck");
        testHealthCheckIndexRequest.source(content);
        client.index(testHealthCheckIndexRequest);

        assertThat(elasticSearchHealthCheck.check().isHealthy()).isTrue();

        elasticSearchContainer.pause();

        assertThat(elasticSearchHealthCheck.check().isUnHealthy()).isTrue();

    }


}
