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

import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class DockerRabbitMQ {

    private static final String DEFAULT_RABBIT_NODE = "my-rabbit";
    private static final int DEFAULT_RABBITMQ_PORT = 5672;
    private static final String RABBITMQ_ERLANG_COOKIE = "RABBITMQ_ERLANG_COOKIE";
    private static final String RABBITMQ_NODENAME = "RABBITMQ_NODENAME";
    private final Optional<String> nodeName;
    private GenericContainer<?> container;
    private Optional<String> hostName;

    public static DockerRabbitMQ withCookieAndNodeName(String hostName, String erlangCookie, String nodeName, Network network) {
        return new DockerRabbitMQ(Optional.ofNullable(hostName), Optional.ofNullable(erlangCookie), Optional.ofNullable(nodeName),
            Optional.of(network));
    }

    public static DockerRabbitMQ withoutCookie() {
        return new DockerRabbitMQ(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @SuppressWarnings("resource")
    private DockerRabbitMQ(Optional<String> hostName, Optional<String> erlangCookie, Optional<String> nodeName, Optional<Network> net) {
        this.hostName = hostName;
        container = new GenericContainer<>("rabbitmq:3.7.3")
                .withCreateContainerCmdModifier(cmd -> cmd.withName(hostName.orElse("localhost")))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(hostName.orElse(DEFAULT_RABBIT_NODE)))
                .withExposedPorts(DEFAULT_RABBITMQ_PORT)
                .waitingFor(new RabbitMQWaitStrategy())
                .withLogConsumer(frame -> LoggerFactory.getLogger(DockerRabbitMQ.class).debug(frame.getUtf8String()));
        net.ifPresent(network -> container.withNetwork(network));
        erlangCookie.ifPresent(cookie -> container.withEnv(RABBITMQ_ERLANG_COOKIE, cookie));
        nodeName.ifPresent(name -> container.withEnv(RABBITMQ_NODENAME, name));
        this.nodeName = nodeName;
    }

    public String getHostIp() {
        return container.getContainerInfo().getNetworkSettings().getIpAddress();
    }

    public Integer getPort() {
        return container.getMappedPort(DEFAULT_RABBITMQ_PORT);
    }

    public String getUsername() {
        return "guest";
    }

    public String getPassword() {
        return "guest";
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public GenericContainer<?> container() {
        return container;
    }

    public String node() {
        return nodeName.get();
    }

    public void join(DockerRabbitMQ rabbitMQ) throws Exception {
        String stdout = container()
                .execInContainer("rabbitmqctl", "stop_app")
                .getStdout();
        System.out.println(stdout);
        String stdout2 = container()
                .execInContainer("rabbitmqctl", "join_cluster", rabbitMQ.node())
                .getStdout();
        System.out.println(stdout2);
    }
}
