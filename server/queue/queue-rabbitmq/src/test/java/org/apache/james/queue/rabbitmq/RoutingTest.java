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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Throwables;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@ExtendWith(DockerRabbitMQExtention.class)
public class RoutingTest {

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
    public static final String CONVERSATION_1 = "c1";
    public static final String CONVERSATION_2 = "c2";
    public static final String CONVERSATION_3 = "c3";
    public static final String CONVERSATION_4 = "c4";

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
        ConcurrentLinkedQueue<Integer> results1 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> results2 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> results3 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> results4 = new ConcurrentLinkedQueue<>();

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

            String queue1 = channel1.queueDeclare().getQueue();
            // 1 will follow discussion 1 and 2
            channel1.queueBind(queue1, EXCHANGE_NAME, CONVERSATION_1);
            channel1.queueBind(queue1, EXCHANGE_NAME, CONVERSATION_2);

            String queue2 = channel2.queueDeclare().getQueue();
            // 2 will follow discussion 3 and 2
            channel2.queueBind(queue2, EXCHANGE_NAME, CONVERSATION_3);
            channel2.queueBind(queue2, EXCHANGE_NAME, CONVERSATION_2);

            String queue3 = channel3.queueDeclare().getQueue();
            // 1 will follow discussion 3 and 4
            channel3.queueBind(queue3, EXCHANGE_NAME, CONVERSATION_3);
            channel3.queueBind(queue3, EXCHANGE_NAME, CONVERSATION_4);

            String queue4 = channel4.queueDeclare().getQueue();
            // 1 will follow discussion 1 and 2
            channel4.queueBind(queue4, EXCHANGE_NAME, CONVERSATION_1);
            channel4.queueBind(queue4, EXCHANGE_NAME, CONVERSATION_4);

            // 1 will produce 100 messages
            String message1 = "1";
            channel1.basicPublish(EXCHANGE_NAME, CONVERSATION_1, NO_PROPERTIES,
                message1.getBytes(StandardCharsets.UTF_8));
            String message2 = "2";
            channel2.basicPublish(EXCHANGE_NAME, CONVERSATION_2, NO_PROPERTIES,
                message2.getBytes(StandardCharsets.UTF_8));
            String message3 = "3";
            channel3.basicPublish(EXCHANGE_NAME, CONVERSATION_3, NO_PROPERTIES,
                message3.getBytes(StandardCharsets.UTF_8));
            String message4 = "4";
            channel4.basicPublish(EXCHANGE_NAME, CONVERSATION_4, NO_PROPERTIES,
                message4.getBytes(StandardCharsets.UTF_8));

            channel1.basicConsume(queue1, storeInResultCallBack(channel1, results1));
            channel2.basicConsume(queue2, storeInResultCallBack(channel2, results2));
            channel3.basicConsume(queue3, storeInResultCallBack(channel3, results3));
            channel4.basicConsume(queue4, storeInResultCallBack(channel4, results4));
            Thread.sleep(1000 * 5);

           assertThat(results1).containsOnly(1, 2);
           assertThat(results2).containsOnly(2, 3);
           assertThat(results3).containsOnly(3, 4);
           assertThat(results4).containsOnly(1, 4);
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
