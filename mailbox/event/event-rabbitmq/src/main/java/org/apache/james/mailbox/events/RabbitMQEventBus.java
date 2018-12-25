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

package org.apache.james.mailbox.events;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.james.backend.rabbitmq.RabbitMQConnectionFactory;
import org.apache.james.event.json.EventSerializer;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;

import com.rabbitmq.client.Connection;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

class RabbitMQEventBus implements EventBus {

    static final String MAILBOX_EVENT = "mailboxEvent";
    static final String MAILBOX_EVENT_EXCHANGE_NAME = MAILBOX_EVENT + "-exchange";
    static final String EMPTY_ROUTING_KEY = "";

    static final boolean DURABLE = true;
    private static final String DIRECT_EXCHANGE = "direct";

    private final EventSerializer eventSerializer;
    private final Sender sender;
    private final Mono<Connection> connectionMono;
    private final Map<Group, GroupRegistration> groupRegistrations;

    RabbitMQEventBus(RabbitMQConnectionFactory rabbitMQConnectionFactory, EventSerializer eventSerializer) {
        this.connectionMono = Mono.fromSupplier(rabbitMQConnectionFactory::create).cache();
        this.eventSerializer = eventSerializer;
        this.sender = RabbitFlux.createSender(new SenderOptions().connectionMono(connectionMono));
        this.groupRegistrations = new ConcurrentHashMap<>();
    }

    public Mono<Void> start() {
        return sender.declareExchange(ExchangeSpecification.exchange(MAILBOX_EVENT_EXCHANGE_NAME)
                .durable(DURABLE)
                .type(DIRECT_EXCHANGE))
            .subscribeOn(Schedulers.elastic())
            .then();
    }

    @PreDestroy
    public void stop() {
        sender.close();
        groupRegistrations.values().forEach(GroupRegistration::unregister);
    }

    @Override
    public Registration register(MailboxListener listener, RegistrationKey key) {
        throw new NotImplementedException("will implement latter");
    }

    @Override
    public Registration register(MailboxListener listener, Group group) {
        return groupRegistrations
            .compute(group, (groupToRegister, oldGroupRegistration) -> {
                if (oldGroupRegistration != null) {
                    throw new GroupAlreadyRegistered(group);
                }
                return newRegistrationGroup(listener, groupToRegister);
            })
            .start();
    }

    private GroupRegistration newRegistrationGroup(MailboxListener listener, Group group) {
        return new GroupRegistration(
            connectionMono,
            sender,
            eventSerializer,
            listener,
            group,
            () -> groupRegistrations.remove(group));
    }

    @Override
    public Mono<Void> dispatch(Event event, Set<RegistrationKey> key) {
        Mono<OutboundMessage> outboundMessage = Mono.just(event)
            .publishOn(Schedulers.parallel())
            .map(this::serializeEvent)
            .map(payload -> new OutboundMessage(MAILBOX_EVENT_EXCHANGE_NAME, EMPTY_ROUTING_KEY, payload));

        Mono<Void> publishMono = sender.send(outboundMessage).cache();
        publishMono.subscribe();
        return publishMono;
    }

    private byte[] serializeEvent(Event event) {
        return eventSerializer.toJson(event).getBytes(StandardCharsets.UTF_8);
    }
}
