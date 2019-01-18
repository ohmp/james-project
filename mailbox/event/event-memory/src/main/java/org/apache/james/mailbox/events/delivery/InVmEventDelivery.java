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

package org.apache.james.mailbox.events.delivery;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.events.EventDeadLetters;
import org.apache.james.mailbox.events.MemoryEventDeadLetters;
import org.apache.james.mailbox.events.RetryBackoffConfiguration;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Schedulers;

public class InVmEventDelivery implements EventDelivery {



    private static final Logger LOGGER = LoggerFactory.getLogger(InVmEventDelivery.class);
    private static final Duration MAX_BACKOFF = Duration.ofMillis(Long.MAX_VALUE);

    private final MetricFactory metricFactory;
    private final RetryBackoffConfiguration retryBackoff;
    private final EventDeadLetters eventDeadLetters;

    @Inject
    @VisibleForTesting
    public InVmEventDelivery(MetricFactory metricFactory, RetryBackoffConfiguration retryBackoff, EventDeadLetters eventDeadLetters) {
        this.metricFactory = metricFactory;
        this.retryBackoff = retryBackoff;
        this.eventDeadLetters = eventDeadLetters;
    }

    @VisibleForTesting
    public InVmEventDelivery(MetricFactory metricFactory) {
        this(metricFactory, RetryBackoffConfiguration.DEFAULT, new MemoryEventDeadLetters());
    }

    @Override
    public ExecutionStages deliver(MailboxListener listener, Event event, EventDelivery.DeliveryOption option) {
        Mono<Void> delivery = option.getRetrier().run(() -> doDeliverToListener(listener, event))
            .onErrorResume(e -> option.getPermanentFailureHandler().handle(event));

        if (listener.getExecutionMode() == MailboxListener.ExecutionMode.SYNCHRONOUS) {
            return new ExecutionStages(delivery, Mono.empty());
        }
        return new ExecutionStages(Mono.empty(), delivery);
    }

    private ExecutionStages deliverByOption(Collection<DeliverableListener> deliverableListeners, Event event, DeliveryOption deliveryOption) {
        Mono<Void> synchronousListeners = doDeliver(
            filterByExecutionMode(deliverableListeners, MailboxListener.ExecutionMode.SYNCHRONOUS), event, deliveryOption)
            .subscribeWith(MonoProcessor.create());
        Mono<Void> asyncListener = doDeliver(
            filterByExecutionMode(deliverableListeners, MailboxListener.ExecutionMode.ASYNCHRONOUS), event, deliveryOption)
            .subscribeWith(MonoProcessor.create());

        return new ExecutionStages(synchronousListeners, asyncListener);
    }

    private Stream<DeliverableListener> filterByExecutionMode(Collection<DeliverableListener> deliverableListeners, MailboxListener.ExecutionMode executionMode) {
        return deliverableListeners.stream()
            .filter(deliverableListener -> deliverableListener.getMailboxListener().getExecutionMode() == executionMode);
    }

    private Mono<Void> doDeliver(Stream<DeliverableListener> deliverableListeners, Event event, DeliveryOption deliveryOption) {
        return Flux.fromStream(deliverableListeners)
            .flatMap(deliverableListener -> deliveryWithRetries(event, deliverableListener, deliveryOption))
            .then()
            .subscribeOn(Schedulers.elastic());
    }

    private Mono<Void> deliveryWithRetries(Event event, DeliverableListener deliverableListener, DeliveryOption deliveryOption) {
        Mono<Void> firstDelivery = Mono.fromRunnable(() -> doDeliverToListener(deliverableListener.getMailboxListener(), event))
            .doOnError(throwable -> LOGGER.error("Error while processing listener {} for {}",
                listenerName(deliverableListener.getMailboxListener()),
                eventName(event),
                throwable))
            .then();

        if (deliveryOption == DeliveryOption.NO_RETRY) {
            return firstDelivery;
        }

        return firstDelivery
            .retryBackoff(retryBackoff.getMaxRetries(), retryBackoff.getFirstBackoff(), MAX_BACKOFF, retryBackoff.getJitterFactor())
            .doOnError(throwable -> LOGGER.error("listener {} exceeded maximum retry({}) to handle event {}",
                listenerName(deliverableListener.getMailboxListener()),
                retryBackoff.getMaxRetries(),
                eventName(event),
                throwable))
            .onErrorResume(throwable -> storeToDeadLetters(event, deliverableListener))
            .then();
    }

    private Mono<Void> storeToDeadLetters(Event event, DeliverableListener deliverableListener) {
        return Mono.justOrEmpty(deliverableListener.getGroup())
            .map(group -> eventDeadLetters.store(group, event))
            .then();
    }

    private void doDeliverToListener(MailboxListener mailboxListener, Event event) {
        TimeMetric timer = metricFactory.timer("mailbox-listener-" + mailboxListener.getClass().getSimpleName());
        try {
            mailboxListener.event(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            timer.stopAndPublish();
        }
    }

    private String listenerName(MailboxListener mailboxListener) {
        return mailboxListener.getClass().getCanonicalName();
    }

    private String eventName(Event event) {
        return event.getClass().getCanonicalName();
    }
}
