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

import static org.apache.james.backend.rabbitmq.Constants.DIRECT_EXCHANGE;
import static org.apache.james.backend.rabbitmq.Constants.DURABLE;
import static org.apache.james.mailbox.events.RabbitMQEventBus.EVENT_BUS_ID;
import static org.apache.james.mailbox.events.RabbitMQEventBus.MAILBOX_EVENT_EXCHANGE_NAME;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.james.event.json.EventSerializer;
import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.AMQP;

import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

public class EventDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventDispatcher.class);

    private final EventSerializer eventSerializer;
    private final Sender sender;
    private final MailboxListenerRegistry mailboxListenerRegistry;
    private final AMQP.BasicProperties basicProperties;
    private final Object lock = new Object();
    private EmitterProcessor<OutboundMessage> emitterProcessor;
    private Disposable consumerSubscripotion;

    EventDispatcher(EventBusId eventBusId, EventSerializer eventSerializer, Sender sender, MailboxListenerRegistry mailboxListenerRegistry) {
        this.eventSerializer = eventSerializer;
        this.sender = sender;
        this.mailboxListenerRegistry = mailboxListenerRegistry;
        this.basicProperties = new AMQP.BasicProperties.Builder()
            .headers(ImmutableMap.of(EVENT_BUS_ID, eventBusId.asString()))
            .build();
    }

    void start() {
        sender.declareExchange(ExchangeSpecification.exchange(MAILBOX_EVENT_EXCHANGE_NAME)
            .durable(DURABLE)
            .type(DIRECT_EXCHANGE))
            .block();

        emitterProcessor = EmitterProcessor.create();
        consumerSubscripotion = sender.send(emitterProcessor.publish().autoConnect()).subscribe();
    }

    void stop() {
        emitterProcessor.dispose();
        consumerSubscripotion.dispose();
    }

    Mono<Void> dispatch(Event event, Set<RegistrationKey> keys) {
        Mono<Void> localListenerDelivery = Flux.fromIterable(keys)
            .subscribeOn(Schedulers.elastic())
            .flatMap(mailboxListenerRegistry::getLocalMailboxListeners)
            .filter(mailboxListener -> mailboxListener.getExecutionMode().equals(MailboxListener.ExecutionMode.SYNCHRONOUS))
            .flatMap(mailboxListener -> Mono.fromRunnable(Throwing.runnable(() -> mailboxListener.event(event)))
                .doOnError(e -> LOGGER.error("Exception happens when handling event of user {}", event.getUser().asString(), e))
                .onErrorResume(e -> Mono.empty()))
            .then();

        Mono<byte[]> serializedEvent = Mono.just(event)
            .publishOn(Schedulers.parallel())
            .map(this::serializeEvent)
            .cache();

        Mono<Void> distantDispatchMono = doDispatch(serializedEvent, keys).cache();

        return Flux.concat(localListenerDelivery, distantDispatchMono)
            .subscribeWith(MonoProcessor.create());
    }

    private Mono<Void> doDispatch(Mono<byte[]> serializedEvent, Set<RegistrationKey> keys) {
        Stream<RoutingKeyConverter.RoutingKey> routingKeyStream = Stream.concat(
            Stream.of(RoutingKeyConverter.RoutingKey.empty()),
            keys.stream().map(RoutingKeyConverter.RoutingKey::of));

        return doDispatch(serializedEvent, MAILBOX_EVENT_EXCHANGE_NAME, routingKeyStream, basicProperties);
    }

    Mono<Void> doDispatch(Mono<byte[]> serializedEvent, String exchangeName, Stream<RoutingKeyConverter.RoutingKey> routingKeyStream, AMQP.BasicProperties properties) {
        Stream<OutboundMessage> outboundMessages = routingKeyStream
            .map(routingKey -> new OutboundMessage(exchangeName, routingKey.asString(), properties, serializedEvent.block()));

        return Mono.fromRunnable(() -> {
            synchronized (lock) {
                outboundMessages.forEach(emitterProcessor::onNext);
            }
        });
    }

    private byte[] serializeEvent(Event event) {
        return eventSerializer.toJson(event).getBytes(StandardCharsets.UTF_8);
    }
}
