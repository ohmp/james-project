/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.james.modules.RabbitMQExtension;
import org.apache.james.util.concurrent.NamedThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RabbitMQJamesServerWithRetryConnectionTest {
    private static final long WAITING_TIME = Duration.ofSeconds(10).toMillis();

    private RabbitMQExtension rabbitMQExtension = new RabbitMQExtension();
    private ExecutorService executorService;

    @RegisterExtension
    JamesServerExtension jamesServerExtension = CassandraRabbitMQJamesServerFixture
        .baseExtensionBuilder(rabbitMQExtension)
        .disableAutoStart()
        .build();

    @BeforeEach
    void setUp() {
        ThreadFactory threadFactory = NamedThreadFactory.withClassName(getClass());
        executorService = Executors.newFixedThreadPool(1, threadFactory);
    }

    @Test
    void serverShouldStartAtDefault(GuiceJamesServer server) throws Exception {
        server.start();

        assertThat(server.isStarted()).isTrue();
    }

    @Test
    void serverShouldRetryToConnectToCassandraWhenStartService(GuiceJamesServer server) throws Exception {
        rabbitMQExtension.dockerRabbitMQ().pause();
        waitBeforeExecution(WAITING_TIME, () -> rabbitMQExtension.dockerRabbitMQ().unpause());

        server.start();

        assertThat(server.isStarted()).isTrue();
    }

    private void waitBeforeExecution(long waitingTime, Runnable action) {
        executorService.submit(() -> {
            try {
                Thread.sleep(waitingTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            action.run();
        });
    }
}
