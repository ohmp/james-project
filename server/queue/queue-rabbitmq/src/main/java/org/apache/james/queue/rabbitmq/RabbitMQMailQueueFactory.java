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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;

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
    private final URI rabbitManagementUri;
    private final ObjectMapper objectMapper;
    private final BlobStore blobStore;

    private final Map<String, RabbitMQMailQueue> mailQueueCache;

    public RabbitMQMailQueueFactory(Connection connection, URI rabbitManagementUri, BlobStore blobStore) throws IOException {
        this.connection = connection;
        this.rabbitManagementUri = rabbitManagementUri;
        this.blobStore = blobStore;
        this.objectMapper = new ObjectMapper();
        this.mailQueueCache = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<MailQueue> getQueue(String name) {
        return listCreatedMailQueues()
            .stream()
            .filter(queue -> queue.getName().equals(name))
            .findFirst();
    }

    @Override
    public MailQueue createQueue(String name) {
        return getQueue(name)
            .orElseGet(() -> cacheAwareCreate(name));
    }

    private MailQueue cacheAwareCreate(String queueName) {
        return mailQueueCache.computeIfAbsent(queueName, name -> new RabbitMQMailQueue(name,
            createChannelForQueue(name),
            exchangeForMailQueue(name),
            blobStore));
    }

    private Channel createChannelForQueue(String name) {
        try {
            String exchange = exchangeForMailQueue(name);
            String workQueueForMailQueue = workQueueForMailQueue(name);
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "direct", DURABLE);
            channel.queueDeclare(workQueueForMailQueue, DURABLE, !EXCLUSIVE, AUTO_DELETE, ImmutableMap.of());
            channel.queueBind(workQueueForMailQueue, exchange, ROUTING_KEY);
            return channel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());
        return jsonNode.findValues("name")
            .stream()
            .map(JsonNode::textValue)
            .filter(this::isMailQueueName)
            .map(this::toMailqueueName)
            .map(this::cacheAwareCreate)
            .collect(Guavate.toImmutableSet());
    }

    private String toMailqueueName(String name) {
        return name.substring(WORKQUEUE_PREFIX.length());
    }

    private boolean isMailQueueName(String name) {
        return name.startsWith(WORKQUEUE_PREFIX);
    }
}
