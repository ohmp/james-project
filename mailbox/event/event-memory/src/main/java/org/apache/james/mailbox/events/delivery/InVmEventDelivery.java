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
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class InVmEventDelivery implements EventDelivery {
    private static final Logger LOGGER = LoggerFactory.getLogger(InVmEventDelivery.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    private static final int DEFAULT_RETRIES = 3;

    private final MetricFactory metricFactory;
    private final Duration executionTimeout;
    private final int executionRetries;

    @Inject
    public InVmEventDelivery(MetricFactory metricFactory) {
        this(metricFactory, DEFAULT_TIMEOUT, DEFAULT_RETRIES);
    }

    public InVmEventDelivery(MetricFactory metricFactory, Duration executionTimeout, int executionRetries) {
        this.metricFactory = metricFactory;
        this.executionTimeout = executionTimeout;
        this.executionRetries = executionRetries;
    }

    @Override
    public ExecutionStages deliver(Collection<MailboxListener> mailboxListeners, Event event) {
        Mono<Void> synchronousListeners = doDeliver(
            filterByExecutionMode(mailboxListeners, MailboxListener.ExecutionMode.SYNCHRONOUS), event)
            .cache();
        Mono<Void> asyncListener = doDeliver(
            filterByExecutionMode(mailboxListeners, MailboxListener.ExecutionMode.ASYNCHRONOUS), event)
            .cache();

        synchronousListeners.subscribe();
        asyncListener.subscribe();

        return new ExecutionStages(synchronousListeners, asyncListener);
    }

    private ImmutableList<MailboxListener> filterByExecutionMode(Collection<MailboxListener> mailboxListeners, MailboxListener.ExecutionMode executionMode) {
        return mailboxListeners.stream()
            .filter(listener -> listener.getExecutionMode() == executionMode)
            .collect(Guavate.toImmutableList());
    }

    private Mono<Void> doDeliver(Collection<MailboxListener> mailboxListeners, Event event) {
        return Flux.fromIterable(mailboxListeners)
            .flatMap(mailboxListener -> deliverToListenerReliably(mailboxListener, event))
            .then()
            .subscribeOn(Schedulers.elastic());
    }

    private Mono<Object> deliverToListenerReliably(MailboxListener mailboxListener, Event event) {
        return Mono.fromRunnable(() -> doDeliverToListener(mailboxListener, event))
            .publishOn(Schedulers.elastic())
            .timeout(executionTimeout)
            .retryWhen(companion -> companion.zipWith(Flux.range(0, executionRetries),
                (error, index) -> {
                    if (error instanceof TimeoutException) {
                        if (index < executionRetries) {
                            return index;
                        }
                    }
                    LOGGER.warn("Error while execution {}", mailboxListener, error);
                    throw Exceptions.propagate(error);
                })
                .flatMap(index -> Mono.delay(Duration.ofSeconds(index))));
    }

    private void doDeliverToListener(MailboxListener mailboxListener, Event event) {
        TimeMetric timer = metricFactory.timer("mailbox-listener-" + mailboxListener.getClass().getSimpleName());
        try {
            mailboxListener.event(event);
        } catch (Throwable throwable) {
            LOGGER.error("Error while processing listener {} for {}",
                mailboxListener.getClass().getCanonicalName(), event.getClass().getCanonicalName(),
                throwable);
        } finally {
            timer.stopAndPublish();
        }
    }

}
