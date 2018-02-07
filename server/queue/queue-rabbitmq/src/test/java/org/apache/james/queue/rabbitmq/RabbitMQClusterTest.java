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
package org.apache.james.queue.rabbitmq;

import org.apache.james.queue.rabbitmq.DockerClusterRabbitMQExtention.DockerRabbitMQCluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DockerClusterRabbitMQExtention.class)
public class RabbitMQClusterTest {

    private DockerRabbitMQCluster cluster;

    @BeforeEach
    public void setup(DockerRabbitMQCluster cluster) {
        this.cluster = cluster;
    }

    @Test
    public void rabbitMQManagerShouldReturnThreeNodesWhenAskingForStatus() throws Exception {
        String stdout2 = cluster.getRabbitMQ2().container()
            .execInContainer("nslookup", "rabbit1")
            .getStdout();
        System.out.println(stdout2);

        Thread.sleep(10 * 1000);

        String stdout = cluster.getRabbitMQ1().container()
            .execInContainer("rabbitmqctl", "cluster_status")
            .getStdout();
        System.out.println(stdout);
    }
}
