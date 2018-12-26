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

import org.apache.james.backend.rabbitmq.RabbitMQConnectionFactory;
import org.apache.james.event.json.EventSerializer;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;

import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.RabbitFlux;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.ReceiverOptions;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.SenderOptions;

class RabbitMQEventBus implements EventBus {

    static final String MAILBOX_EVENT = "mailboxEvent";
    static final String MAILBOX_EVENT_EXCHANGE_NAME = MAILBOX_EVENT + "-exchange";
    static final String EMPTY_ROUTING_KEY = "";

    static final boolean DURABLE = true;
    private static final String DIRECT_EXCHANGE = "direct";

    private static final boolean AUTO_DELETE = true;
    private static final boolean EXCLUSIVE = true;
    private final ImmutableMap<String, Object> NO_ARGUMENTS = ImmutableMap.of();

    private final EventSerializer eventSerializer;
    private final Sender sender;
    private final Mono<Connection> connectionMono;
    private final Map<Group, GroupRegistration> groupRegistrations;
    private final MailboxListenerRegistry mailboxListenerRegistry;
    private final RoutingKeyConverter routingKeyConverter;
    private String registrationQueue;
    private Receiver registrationReciever;

    RabbitMQEventBus(RabbitMQConnectionFactory rabbitMQConnectionFactory, EventSerializer eventSerializer, RoutingKeyConverter routingKeyConverter) {
        this.connectionMono = Mono.fromSupplier(rabbitMQConnectionFactory::create).cache();
        this.eventSerializer = eventSerializer;
        this.routingKeyConverter = routingKeyConverter;
        this.sender = RabbitFlux.createSender(new SenderOptions().connectionMono(connectionMono));
        this.groupRegistrations = new ConcurrentHashMap<>();
        this.mailboxListenerRegistry = new MailboxListenerRegistry();
    }

    public Mono<Void> start() {
        registrationQueue = sender.declareQueue(QueueSpecification.queue()
            .durable(DURABLE)
            .exclusive(EXCLUSIVE)
            .autoDelete(AUTO_DELETE)
            .arguments(NO_ARGUMENTS))
            .map(AMQP.Queue.DeclareOk::getQueue)
            .block();
        registrationReciever = RabbitFlux.createReceiver(new ReceiverOptions().connectionMono(connectionMono));


        registrationReciever.consumeAutoAck(registrationQueue)
            .subscribeOn(Schedulers.parallel())
            .flatMap(this::handleDelivery)
            .subscribe();

        return sender.declareExchange(ExchangeSpecification.exchange(MAILBOX_EVENT_EXCHANGE_NAME)
                .durable(DURABLE)
                .type(DIRECT_EXCHANGE))
            .subscribeOn(Schedulers.elastic())
            .then();
    }

    private Mono<Void> handleDelivery(Delivery delivery) {
        if (delivery.getBody() == null) {
            return Mono.empty();
        }
        String routingKey = delivery.getEnvelope().getRoutingKey();
        RegistrationKey registrationKey = routingKeyConverter.toRegistrationKey(routingKey);
        Event event = eventSerializer.fromJson(new String(delivery.getBody(), StandardCharsets.UTF_8)).get();

        return mailboxListenerRegistry.getLocalMailboxListeners(registrationKey)
            .flatMap(listener -> Mono.fromRunnable(() -> listener.event(event)))
            .subscribeOn(Schedulers.elastic())
            .then();
    }

    @PreDestroy
    public void stop() {
        registrationReciever.close();
        sender.delete(QueueSpecification.queue(registrationQueue)).block();
        sender.close();
        groupRegistrations.values().forEach(GroupRegistration::unregister);
    }

    @Override
    public Registration register(MailboxListener listener, RegistrationKey key) {
        KeyRegistration keyRegistration = new KeyRegistration(sender, key, registrationQueue, () -> mailboxListenerRegistry.removeListener(key, listener));
        keyRegistration.createRegistrationBinding().block();
        mailboxListenerRegistry.addListener(key, listener);
        return keyRegistration;
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
    public Mono<Void> dispatch(Event event, Set<RegistrationKey> keys) {
        Mono<byte[]> serializedEvent = Mono.just(event)
                    .publishOn(Schedulers.parallel())
                    .map(this::serializeEvent)
                    .cache();

        Mono<Void> dispatchMono = doDispatch(serializedEvent, keys).cache();
        dispatchMono.subscribe();

        return dispatchMono;
    }

    private Mono<Void> doDispatch(Mono<byte[]> serializedEvent, Set<RegistrationKey> keys) {
        return Flux.concat(
            Mono.just(EMPTY_ROUTING_KEY),
            Flux.fromIterable(keys)
                        .map(RoutingKeyConverter::toRoutingKey))
            .map(routingKey -> serializedEvent
                .map(payload -> new OutboundMessage(MAILBOX_EVENT_EXCHANGE_NAME, routingKey, payload)))
            .flatMap(sender::send)
            .then();
    }

    private byte[] serializeEvent(Event event) {
        return eventSerializer.toJson(event).getBytes(StandardCharsets.UTF_8);
    }
}
