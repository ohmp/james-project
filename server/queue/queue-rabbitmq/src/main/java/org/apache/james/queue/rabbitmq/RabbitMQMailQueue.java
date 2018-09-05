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
import javax.mail.internet.AddressException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.core.MailAddress;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.server.core.MailImpl;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.james.util.mime.MessageSplitter;
import org.apache.mailet.Mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.lambdas.Throwing;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

public class RabbitMQMailQueue implements MailQueue {
    public static final boolean AUTO_ACK = true;
    public static final boolean MULTIPLE = true;
    private final String name;
    private final Channel channel;
    private final String exchangeName;
    private final String workQueueName;
    private final BlobStore blobStore;
    private final ObjectMapper objectMapper;

    public RabbitMQMailQueue(String name, Channel channel, String exchangeName, String workQueueName, BlobStore blobStore) {
        this.name = name;
        this.channel = channel;
        this.exchangeName = exchangeName;
        this.workQueueName = workQueueName;
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

        System.out.println(new String(message));

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
            channel.basicPublish(exchangeName, RabbitMQMailQueueFactory.ROUTING_KEY, new AMQP.BasicProperties(), message);
        } catch (IOException e) {
            throw new MailQueueException("Unable to publish to RabbitMQ", e);
        }
    }

    @Override
    public MailQueueItem deQueue() throws MailQueueException {
        GetResponse getResponse = readChannel();
        MailDTO mailDTO = toDTO(getResponse);
        Mail mail = toMail(mailDTO);

        return new MailQueueItem() {
            @Override
            public Mail getMail() {
                return mail;
            }

            @Override
            public void done(boolean success) throws MailQueueException {
                try {
                    channel.basicAck(getResponse.getEnvelope().getDeliveryTag(), !MULTIPLE);
                } catch (IOException e) {
                    throw new MailQueueException("Failed to ACK", e);
                }
            }
        };
    }

    private MailDTO toDTO(GetResponse getResponse) throws MailQueueException {
        try {
            return objectMapper.readValue(getResponse.getBody(), MailDTO.class);
        } catch (IOException e) {
            throw new MailQueueException("Failed to parse DTO", e);
        }
    }

    private GetResponse readChannel() throws MailQueueException {
        try {
            GetResponse getResponse;
            do {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getResponse = channel.basicGet(workQueueName, !AUTO_ACK);
            } while (getResponse.getBody() == null);
            return getResponse;
        } catch (IOException e) {
            throw new MailQueueException("Failed to read channel", e);
        }
    }

    private Mail toMail(MailDTO dto) throws MailQueueException {
        try {
            return new MailImpl(
                dto.getName(),
                new MailAddress(dto.getSender()),
                dto.getRecipients()
                    .stream()
                    .map(Throwing.<String, MailAddress>function(MailAddress::new).sneakyThrow())
                    .collect(ImmutableList.toImmutableList()));
        } catch (AddressException e) {
            throw new MailQueueException("Failed to parse mail address", e);
        }
    }
}