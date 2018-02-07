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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@ExtendWith(DockerRabbitMQExtention.class)
public class RabbitMQTest {

    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);
    private ConnectionFactory connectionFactory;
    private DockerRabbitMQ rabbitMQ;

    @BeforeEach
    public void setup(DockerRabbitMQ rabbitMQ) {
        this.rabbitMQ = rabbitMQ;
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
    public void demonstrateDurability() throws Exception {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel();) {
            String exchangeName = "exchangeName";
            String routingKey = "routingKey";
            String queueName = createQueue(channel, exchangeName, routingKey);

            publishAMessage(channel, exchangeName, routingKey);

            rabbitMQ.restart();

            Thread.sleep(ONE_MINUTE);
            boolean autoAck = false;
            assertThat(channel.basicGet(queueName, autoAck)).isNotNull();
        }
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
