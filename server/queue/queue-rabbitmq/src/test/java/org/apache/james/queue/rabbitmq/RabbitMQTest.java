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

import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.AUTO_DELETE;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.DURABLE;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.EXCHANGE_NAME;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.EXCLUSIVE;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.NO_PROPS;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.QUEUE;
import static org.apache.james.queue.rabbitmq.RabbitMQClusterTest.ROUTING_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.github.steveash.guavate.Guavate;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@ExtendWith(DockerRabbitMQExtention.class)
public class RabbitMQTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private ConnectionFactory connectionFactory;

    @BeforeEach
    public void setup(DockerRabbitMQ rabbitMQ) {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHostIp());
        connectionFactory.setPort(rabbitMQ.getPort());
        connectionFactory.setUsername(rabbitMQ.getUsername());
        connectionFactory.setPassword(rabbitMQ.getPassword());
    }

    @Test
    public void publishedEventWithoutSubscriberShouldNotBeLost() throws Exception {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel();) {
            String exchangeName = "exchangeName";
            String routingKey = "routingKey";
            String queueName = createQueue(channel, exchangeName, routingKey);

            publishAMessage(channel, exchangeName, routingKey);

            Thread.sleep(ONE_MINUTE);
            boolean autoAck = false;
            assertThat(channel.basicGet(queueName, autoAck)).isNotNull();
        }
    }

    @Test
    public void test(DockerRabbitMQ rabbitMQ) throws Exception {
        ConcurrentLinkedDeque<Integer> result = new ConcurrentLinkedDeque<>();

        ConnectionFactory connectionFactory1 = createConnectionFactory(rabbitMQ);

        try (Connection connection = connectionFactory1.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel.queueDeclare(QUEUE, DURABLE, !EXCLUSIVE, !AUTO_DELETE, ImmutableMap.of()).getQueue();
            channel.queueBind(QUEUE, EXCHANGE_NAME, ROUTING_KEY);

            Thread.sleep(1000);

            for (int i = 0; i < 10; i++) {
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPS, String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }
        }

        try (Connection connection = connectionFactory1.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclarePassive(QUEUE);
            channel.basicConsume(QUEUE, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    System.out.println("2!");
                    result.add(Integer.valueOf(new String(body, StandardCharsets.UTF_8)));
                }
            });
        }

       /* try (Connection connection = connectionFactory3.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclarePassive(QUEUE);
            channel.basicConsume(QUEUE, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    System.out.println("3!");
                    result.add(Integer.valueOf(new String(body, StandardCharsets.UTF_8)));
                }
            });
        }*/

        Thread.sleep(5 * 1000);


        assertThat(result)
            .containsOnlyElementsOf(
                IntStream.range(0, 10)
                    .boxed()
                    .collect(Guavate.toImmutableList()));
    }

    private ConnectionFactory createConnectionFactory(DockerRabbitMQ rabbitMQ) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHostIp());
        connectionFactory.setPort(rabbitMQ.getPort());
        connectionFactory.setUsername(rabbitMQ.getUsername());
        connectionFactory.setPassword(rabbitMQ.getPassword());
        return connectionFactory;
    }

    private String createQueue(Channel channel, String exchangeName, String routingKey) throws IOException {
        boolean durable = true;
        channel.exchangeDeclare(exchangeName, "direct", durable);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, routingKey);
        return queueName;
    }

    private void publishAMessage(Channel channel, String exchangeName, String routingKey) throws IOException {
        byte[] messageBodyBytes = "Hello, world!".getBytes();
        BasicProperties properties = null;
        channel.basicPublish(exchangeName, routingKey, properties, messageBodyBytes);
    }
}
