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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.james.queue.rabbitmq.DockerClusterRabbitMQExtention.DockerRabbitMQCluster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.github.steveash.guavate.Guavate;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@ExtendWith(DockerClusterRabbitMQExtention.class)
public class RabbitMQClusterTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);
    public static final String EXCHANGE_NAME = "exchangeName";
    public static final String ROUTING_KEY = "routingKey";
    public static final boolean DURABLE = true;
    public static final String QUEUE = "queue";
    public static final boolean AUTO_DELETE = true;
    public static final boolean EXCLUSIVE = true;
    public static final AMQP.BasicProperties NO_PROPS = null;
    public static final int MESSAGE_COUNT = 10;

    @Test
    public void rabbitMQManagerShouldReturnThreeNodesWhenAskingForStatus(DockerRabbitMQCluster cluster) throws Exception {
        String stdout = cluster.getRabbitMQ1().container()
            .execInContainer("rabbitmqctl", "cluster_status")
            .getStdout();
        System.out.println(stdout);

        assertThat(stdout)
            .contains(
                DockerClusterRabbitMQExtention.RABBIT_1,
                DockerClusterRabbitMQExtention.RABBIT_2,
                DockerClusterRabbitMQExtention.RABBIT_3);
    }

    @Test
    public void queuesShouldBeShared(DockerRabbitMQCluster cluster) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(cluster.getRabbitMQ1());
        ConnectionFactory connectionFactory2 = createConnectionFactory(cluster.getRabbitMQ2());
        ConnectionFactory connectionFactory3 = createConnectionFactory(cluster.getRabbitMQ3());

        try (Connection connection = connectionFactory1.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);


            for (int i = 0; i < MESSAGE_COUNT; i++) {
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }
        }

        try (Connection connection2 = connectionFactory2.newConnection();
             Channel channel2 = connection2.createChannel();
             Connection connection3 = connectionFactory3.newConnection();
             Channel channel3 = connection3.createChannel()) {

            channel2.basicConsume(QUEUE, callback(result, channel2));
            channel3.basicConsume(QUEUE, callback(result, channel3));
            Thread.sleep(5 * 1000);
        }

        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, MESSAGE_COUNT)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    @Test
    public void queuesShouldBeDeclarableOnAnotherNode(DockerRabbitMQCluster cluster) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(cluster.getRabbitMQ1());
        ConnectionFactory connectionFactory2 = createConnectionFactory(cluster.getRabbitMQ2());

        try (Connection connection = connectionFactory1.newConnection();
             Channel channel = connection.createChannel();
             Connection connection2 = connectionFactory2.newConnection();
             Channel channel2 = connection2.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel2.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel2.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);

            for (int i = 0; i < MESSAGE_COUNT; i++) {
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            channel2.basicConsume(QUEUE, callback(result, channel2));
            Thread.sleep(5 * 1000);
        }

        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, MESSAGE_COUNT)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    @Test
    public void nodeKillingWhenProducing(DockerRabbitMQCluster cluster) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(cluster.getRabbitMQ1());
        ConnectionFactory connectionFactory2 = createConnectionFactory(cluster.getRabbitMQ2());

        System.out.println(cluster.getRabbitMQ1().getHostIp());
        ImmutableList<Address> addresses = ImmutableList.of(
            new Address(cluster.getRabbitMQ1().getHostIp(), cluster.getRabbitMQ1().getPort()),
            new Address(cluster.getRabbitMQ2().getHostIp(), cluster.getRabbitMQ2().getPort()),
            new Address(cluster.getRabbitMQ3().getHostIp(), cluster.getRabbitMQ3().getPort()));
        System.out.println(addresses);
        try (Connection connection = connectionFactory1.newConnection(addresses);
             Channel channel = connection.createChannel();
             Connection connection2 = connectionFactory2.newConnection();
             Channel channel2 = connection2.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel2.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel2.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);

            for (int i = 0; i < MESSAGE_COUNT; i++) {
                if (i == MESSAGE_COUNT / 2) {
                    System.out.println("Killing");
                    cluster.getRabbitMQ1().stop();
                }
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            channel2.basicConsume(QUEUE, callback(result, channel2));
            Thread.sleep(5 * 1000);
        }

        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, MESSAGE_COUNT)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    @Test
    public void connectingToAClusterWithAFailedRabbit(DockerRabbitMQCluster cluster) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(cluster.getRabbitMQ1());

        cluster.getRabbitMQ1().stop();

        System.out.println(cluster.getRabbitMQ1().getHostIp());
        ImmutableList<Address> addresses = ImmutableList.of(
            new Address(cluster.getRabbitMQ1().getHostIp(), cluster.getRabbitMQ1().getPort()),
            new Address(cluster.getRabbitMQ2().getHostIp(), cluster.getRabbitMQ2().getPort()),
            new Address(cluster.getRabbitMQ3().getHostIp(), cluster.getRabbitMQ3().getPort()));
        System.out.println(addresses);
        try (Connection connection = connectionFactory1.newConnection(addresses);
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);

            for (int i = 0; i < MESSAGE_COUNT; i++) {
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            channel.basicConsume(QUEUE, callback(result, channel));
            Thread.sleep(5 * 1000);
        }

        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, MESSAGE_COUNT)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    @Test
    public void nodeKillingWhenConsuming(DockerRabbitMQCluster cluster) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(cluster.getRabbitMQ1());
        ConnectionFactory connectionFactory2 = createConnectionFactory(cluster.getRabbitMQ2());

        System.out.println(cluster.getRabbitMQ1().getHostIp());
        ImmutableList<Address> addresses = ImmutableList.of(
            new Address(cluster.getRabbitMQ1().getHostIp(), cluster.getRabbitMQ1().getPort()),
            new Address(cluster.getRabbitMQ2().getHostIp(), cluster.getRabbitMQ2().getPort()),
            new Address(cluster.getRabbitMQ3().getHostIp(), cluster.getRabbitMQ3().getPort()));
        System.out.println(addresses);
        try (Connection connection = connectionFactory1.newConnection(addresses);
             Channel channel = connection.createChannel();
             Connection connection2 = connectionFactory2.newConnection(addresses);
             Channel channel2 = connection2.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel2.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel2.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);

            for (int i = 0; i < MESSAGE_COUNT; i++) {
                if (i == MESSAGE_COUNT / 2) {
                    System.out.println("Killing");
                    cluster.getRabbitMQ1().stop();
                }
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            AtomicInteger counter = new AtomicInteger(0);
            channel2.basicConsume(QUEUE, callbackWithCrash(result, channel2, counter, cluster.getRabbitMQ1()));
            Thread.sleep(5 * 1000);
        }

        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, MESSAGE_COUNT)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    private DefaultConsumer callback(ConcurrentLinkedDeque<Integer> result, Channel channel2) {
        return new DefaultConsumer(channel2) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {

                result.add(Integer.valueOf(new String(body, StandardCharsets.UTF_8)));
            }
        };
    }

    private DefaultConsumer callbackWithCrash(ConcurrentLinkedDeque<Integer> result, Channel channel2, AtomicInteger atomicInteger, DockerRabbitMQ containerToStop) {
        return new DefaultConsumer(channel2) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                if (atomicInteger.incrementAndGet() == 5) {
                    containerToStop.stop();
                }
                result.add(Integer.valueOf(new String(body, StandardCharsets.UTF_8)));
            }
        };
    }

    private ConnectionFactory createConnectionFactory(DockerRabbitMQ rabbitMQ) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHostIp());
        connectionFactory.setPort(rabbitMQ.getPort());
        connectionFactory.setUsername(rabbitMQ.getUsername());
        connectionFactory.setPassword(rabbitMQ.getPassword());
        return connectionFactory;
    }
}
