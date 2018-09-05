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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.mime.MessageSplitter;
import org.apache.mailet.Mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

public class RabbitMQMailQueue implements MailQueue {
    private final String name;
    private final Channel channel;
    private final String exchangeName;
    private final BlobStore blobStore;
    private final ObjectMapper objectMapper;

    public RabbitMQMailQueue(String name, Channel channel, String exchangeName, BlobStore blobStore) {
        this.name = name;
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.blobStore = blobStore;
        objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new GuavaModule());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void enQueue(Mail mail, long delay, TimeUnit unit) throws MailQueueException {
        enQueue(mail);
    }

    @Override
    public void enQueue(Mail mail) throws MailQueueException {
        Pair<BlobId, BlobId> blobIds = saveBlobs(mail).join();
        MailDTO mailDTO = MailDTO.fromMail(mail, blobIds.getLeft(), blobIds.getRight());
        byte[] message = getMessageBytes(mailDTO);

        publish(message);
    }

    private CompletableFuture<Pair<BlobId, BlobId>> saveBlobs(Mail mail) throws MailQueueException {
        try {
            Pair<byte[], byte[]> headerBody = MessageSplitter.splitHeaderBody(mail.getMessage());

            return CompletableFutureUtil.combine(
                blobStore.save(headerBody.getLeft()),
                blobStore.save(headerBody.getRight()),
                Pair::of);
        } catch (IOException | MessagingException e) {
            throw new MailQueueException("Error while saving blob", e);
        }
    }

    private byte[] getMessageBytes(MailDTO mailDTO) throws MailQueueException {
        try {
            return objectMapper.writeValueAsBytes(mailDTO);
        } catch (JsonProcessingException e) {
            throw new MailQueueException("Unable to serialize message", e);
        }
    }

    private void publish(byte[] message) throws MailQueueException {
        try {
            channel.basicPublish(exchangeName, "defaultRoutingKey",new AMQP.BasicProperties(), message);
        } catch (IOException e) {
            throw new MailQueueException("Unable to publish to RabbitMQ", e);
        }
    }

    @Override
    public MailQueueItem deQueue() throws MailQueueException, InterruptedException {
        return null;
    }
}