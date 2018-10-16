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

import org.apache.james.core.healthcheck.HealthCheck;
import org.apache.james.core.healthcheck.Result;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticSearchHealthCheckTest {

    @Mock
    private Client client;

    @Mock
    private AdminClient adminClient;

    @Mock
    private ClusterAdminClient clusterAdminClient;

    private HealthCheck healthCheck;

    @Before
    public void setup() {
        HashSet<IndexName> indexNames = new HashSet<>();
        MockitoAnnotations.initMocks(this);
        when(this.client.admin()).thenReturn(adminClient);
        when(this.adminClient.cluster()).thenReturn(clusterAdminClient);
        healthCheck = new ElasticSearchHealthCheck(client, indexNames, 1000);
    }

    @Test
    public void checkShouldReturnHealthyWhenElasticSearchClusterHealthStatusIsGreen() {
        PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
        responseFuture.onResponse(new FakeClusterHealthResponse());

        when(this.clusterAdminClient.health(any(ClusterHealthRequest.class))).thenReturn(responseFuture);

        Result check = healthCheck.check();
        assertThat(check.isHealthy()).isTrue();
    }

    @Test
    public void checkShouldReturnUnHealthyWhenElasticSearchClusterHealthStatusIsRed() {
        PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
        responseFuture.onResponse(new FakeClusterHealthResponse(ClusterHealthStatus.RED));

        when(this.clusterAdminClient.health(any(ClusterHealthRequest.class))).thenReturn(responseFuture);

        Result check = healthCheck.check();
        assertThat(check.isUnHealthy()).isTrue();
    }

    @Test
    public void checkShouldReturnHealthyWhenElasticSearchClusterHealthStatusIsYellow() {
        PlainActionFuture<ClusterHealthResponse> responseFuture = new PlainActionFuture<>();
        responseFuture.onResponse(new FakeClusterHealthResponse(ClusterHealthStatus.YELLOW));

        when(this.clusterAdminClient.health(any(ClusterHealthRequest.class))).thenReturn(responseFuture);

        Result check = healthCheck.check();
        assertThat(check.isHealthy()).isTrue();
    }

    private class FakeClusterHealthResponse extends ClusterHealthResponse {

        private final ClusterHealthStatus status;

        private FakeClusterHealthResponse() {
            this(ClusterHealthStatus.GREEN);
        }

        private FakeClusterHealthResponse(ClusterHealthStatus clusterHealthStatus) {
            super("fake-cluster", new String[0],
                    new ClusterState(new ClusterName("fake-cluster"), 0, null, null, RoutingTable.builder().build(),
                            DiscoveryNodes.builder().build(),
                            ClusterBlocks.builder().build(), null, false));
            this.status = clusterHealthStatus;
        }

        @Override
        public ClusterHealthStatus getStatus() {
            return this.status;
        }

    }

}
