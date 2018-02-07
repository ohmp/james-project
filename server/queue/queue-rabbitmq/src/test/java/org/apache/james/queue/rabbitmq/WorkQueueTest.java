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
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@ExtendWith(DockerRabbitMQExtention.class)
public class WorkQueueTest {

    public static final String EXCHANGE_NAME = "exchangeName";
    public static final boolean DURABLE = true;
    public static final boolean EXCLUSIVE = true;
    public static final String ROUTING_KEY = "";
    public static final AMQP.BasicProperties NO_PROPERTIES = null;
    public static final boolean AUTO_ACK = true;
    public static final String WORK_QUEUE = "workQueue";
    public static final boolean AUTO_DELETE = true;
    public static final Random RANDOM = new Random();
    private ConnectionFactory connectionFactory1;
    private ConnectionFactory connectionFactory2;
    private ConnectionFactory connectionFactory3;
    private ConnectionFactory connectionFactory4;

    @BeforeEach
    public void setup(DockerRabbitMQ rabbitMQ) {
        connectionFactory1 = prepareConnectionFactory(rabbitMQ);
        connectionFactory2 = prepareConnectionFactory(rabbitMQ);
        connectionFactory3 = prepareConnectionFactory(rabbitMQ);
        connectionFactory4 = prepareConnectionFactory(rabbitMQ);
    }

    private ConnectionFactory prepareConnectionFactory(DockerRabbitMQ rabbitMQ) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHostIp());
        connectionFactory.setPort(rabbitMQ.getPort());
        connectionFactory.setUsername(rabbitMQ.getUsername());
        connectionFactory.setPassword(rabbitMQ.getPassword());
        return connectionFactory;
    }

    @Test
    public void rabbitMQShouldSupportTheWorkQueueCase() throws Exception {
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();

        try (Connection connection1 = connectionFactory1.newConnection();
             Channel channel1 = connection1.createChannel();
            Connection connection2 = connectionFactory2.newConnection();
             Channel channel2 = connection2.createChannel();
            Connection connection3 = connectionFactory3.newConnection();
             Channel channel3 = connection3.createChannel();
            Connection connection4 = connectionFactory4.newConnection();
             Channel channel4 = connection4.createChannel()) {

            // Declare the exchange and a single queue attached to it.
            channel1.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);
            channel1.queueDeclare(WORK_QUEUE, DURABLE, !EXCLUSIVE, AUTO_DELETE, ImmutableMap.of());
            channel1.queueBind(WORK_QUEUE, EXCHANGE_NAME, ROUTING_KEY);
            // 1 will produce 100 messages
            for (int i = 0; i < 100; i++) {
                channel1.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPERTIES,
                    String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            channel2.basicConsume(WORK_QUEUE, storeInResultCallBack(channel2, results));
            channel3.basicConsume(WORK_QUEUE, storeInResultCallBack(channel3, results));
            channel4.basicConsume(WORK_QUEUE, storeInResultCallBack(channel4, results));
            Thread.sleep(1000 * 20);

            ImmutableList<Integer> expectedResult = IntStream.range(0, 100).boxed().collect(Guavate.toImmutableList());
            assertThat(results)
                .containsOnlyElementsOf(expectedResult);
        }
    }

    private DefaultConsumer storeInResultCallBack(Channel channel, ConcurrentLinkedQueue<Integer> results) {
        int id = RANDOM.nextInt();
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println(id);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
                Integer payload = Integer.valueOf(new String(body, StandardCharsets.UTF_8));
                results.add(payload);
            }
        };
    }
}
