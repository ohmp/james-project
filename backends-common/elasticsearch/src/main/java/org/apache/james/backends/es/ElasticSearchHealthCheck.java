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

import java.util.Set;
import javax.inject.Inject;

import org.apache.james.core.healthcheck.ComponentName;
import org.apache.james.core.healthcheck.HealthCheck;
import org.apache.james.core.healthcheck.Result;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Health check for Elasticsearch
 */
public class ElasticSearchHealthCheck implements HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchHealthCheck.class);
    private static final ComponentName COMPONENT_NAME = new ComponentName("Elasticsearch backend");
    private final Set<IndexName> indexNames;

    private final Client client;

    private final long requestTimeout;

    @Inject
    public ElasticSearchHealthCheck(Client client, Set<IndexName> indexNames, long requestTimeout) {
        this.client = client;
        this.indexNames = indexNames;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public ComponentName componentName() {
        return COMPONENT_NAME;
    }

    @Override
    public Result check() {
        ClusterHealthRequest request =
                Requests.clusterHealthRequest(indexNames.stream().map(IndexName::getValue).toArray(String[]::new));
        ClusterHealthResponse response =
                this.client.admin().cluster().health(request).actionGet(requestTimeout);

        switch (response.getStatus()) {
            case GREEN:
            case YELLOW:
                return Result.healthy(COMPONENT_NAME);
            case RED:
            default:
                return Result.unhealthy(COMPONENT_NAME);
        }

    }
}
