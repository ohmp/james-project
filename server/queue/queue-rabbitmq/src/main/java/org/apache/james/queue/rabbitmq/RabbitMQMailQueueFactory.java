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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.mailet.Mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;


public class RabbitMQMailQueueFactory implements MailQueueFactory<MailQueue> {

    private static final String PREFIX = "JamesMailQueue";
    public static final String WORKQUEUE_PREFIX = PREFIX + "-workqueue-";
    public static final boolean DURABLE = true;
    public static final boolean EXCLUSIVE = true;
    public static final boolean AUTO_DELETE = true;
    public static final String ROUTING_KEY = "";

    private final Connection connection;
    private final Channel channel;
    private final URI rabbitManagementUri;
    private final ObjectMapper objectMapper;

    public RabbitMQMailQueueFactory(Connection connection, URI rabbitManagementUri) throws IOException {
        this.connection = connection;
        this.channel = connection.createChannel();
        this.rabbitManagementUri = rabbitManagementUri;
        objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<MailQueue> getQueue(String name) {
        String exchange = exchangeForMailQueue(name);
        String workQueueForMailQueue = workQueueForMailQueue(name);
        return listCreatedMailQueues().stream().filter(queue -> queue.getName().equals(name)).findFirst();
    }

    @Override
    public MailQueue createQueue(String name) {
        return getQueue(name)
            .orElseGet(() -> attemptQueueCreation(name));
    }

    private MailQueue attemptQueueCreation(String name) {
        String exchange = exchangeForMailQueue(name);
        String workQueueForMailQueue = workQueueForMailQueue(name);
        try {
            channel.exchangeDeclare(exchange, "direct", DURABLE);
            channel.queueDeclare(workQueueForMailQueue, DURABLE, !EXCLUSIVE, AUTO_DELETE, ImmutableMap.of());
            channel.queueBind(workQueueForMailQueue, exchange, ROUTING_KEY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new RabbitMQMailQueue(name);
    }

    private String exchangeForMailQueue(String name) {
        return PREFIX + "-exchange-" + name;
    }

    private String workQueueForMailQueue(String name) {
        return WORKQUEUE_PREFIX + name;
    }

    @Override
    public Set<MailQueue> listCreatedMailQueues() {
        try {
            URI queuesUri = new URIBuilder(rabbitManagementUri).setPath("/api/queues").build();

            Executor executor = Executor.newInstance().auth("guest", "guest");
            return executor.execute(
                Request.Get(queuesUri)
                .setHeader("Content-Type", ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8).toString()))
                .handleResponse(this::handleListMailQueues);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<MailQueue> handleListMailQueues(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("listing queues failed with error " + response.getStatusLine().getReasonPhrase());
        }
        String encoding = Optional
            .ofNullable(response.getEntity().getContentEncoding())
            .map(NameValuePair::getValue)
            .orElse(StandardCharsets.UTF_8.name());
        JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());
        return jsonNode.findValues("name")
            .stream()
            .map(JsonNode::textValue)
            .filter(this::isMailQueueName)
            .map(this::toMailqueueName)
            .map(RabbitMQMailQueue::new)
            .collect(Guavate.toImmutableSet());
    }

    private String toMailqueueName(String name) {
        return name.substring(WORKQUEUE_PREFIX.length());
    }

    private boolean isMailQueueName(String name) {
        return name.startsWith(WORKQUEUE_PREFIX);
    }

    private static class RabbitMQMailQueue implements MailQueue {
        private final String name;

        public RabbitMQMailQueue(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException {

        }

        @Override
        public void enQueue(Mail mail) throws MailQueueException {

        }

        @Override
        public MailQueueItem deQueue() throws MailQueueException, InterruptedException {
            return null;
        }
    }
}
