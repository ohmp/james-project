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

import static org.apache.james.mailbox.events.EventBusTestFixture.EVENT;
import static org.apache.james.mailbox.events.EventBusTestFixture.EVENT_2;
import static org.apache.james.mailbox.events.EventBusTestFixture.EVENT_ID;
import static org.apache.james.mailbox.events.EventBusTestFixture.EVENT_UNSUPPORTED_BY_LISTENER;
import static org.apache.james.mailbox.events.EventBusTestFixture.FIVE_HUNDRED_MS;
import static org.apache.james.mailbox.events.EventBusTestFixture.GROUP_A;
import static org.apache.james.mailbox.events.EventBusTestFixture.GROUP_B;
import static org.apache.james.mailbox.events.EventBusTestFixture.NO_KEYS;
import static org.apache.james.mailbox.events.EventBusTestFixture.ONE_SECOND;
import static org.apache.james.mailbox.events.EventBusTestFixture.newListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.apache.james.core.Username;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.TestId;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import reactor.core.scheduler.Schedulers;

public interface GroupContract {

    interface SingleEventBusGroupContract extends EventBusContract {

        @Test
        default void groupDeliveryShouldNotExceedRate() {
            int eventCount = 50;
            AtomicInteger nbCalls = new AtomicInteger(0);
            AtomicInteger finishedExecutions = new AtomicInteger(0);
            AtomicBoolean rateExceeded = new AtomicBoolean(false);

            eventBus().initialize(new MailboxListener.GroupMailboxListener() {
                @Override
                public Group getDefaultGroup() {
                    return new GenericGroup("group");
                }

                @Override
                public boolean isHandling(Event event) {
                    return true;
                }

                @Override
                public void event(Event event) throws Exception {
                    if (nbCalls.get() - finishedExecutions.get() > EventBus.EXECUTION_RATE) {
                        rateExceeded.set(true);
                    }
                    nbCalls.incrementAndGet();
                    Thread.sleep(Duration.ofMillis(200).toMillis());
                    finishedExecutions.incrementAndGet();

                }
            }, GROUP_A);

            IntStream.range(0, eventCount)
                .forEach(i -> eventBus().dispatch(EVENT, NO_KEYS).block());

            getSpeedProfile().shortWaitCondition().atMost(org.awaitility.Duration.TEN_MINUTES)
                .untilAsserted(() -> assertThat(finishedExecutions.get()).isEqualTo(eventCount));
            assertThat(rateExceeded).isFalse();
        }

        @Test
        default void groupNotificationShouldDeliverASingleEventToAllListenersAtTheSameTime() {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                ConcurrentLinkedQueue<String> threads = new ConcurrentLinkedQueue<>();
                eventBus().initialize(new MailboxListener.GroupMailboxListener() {
                    @Override
                    public Group getDefaultGroup() {
                        return new GenericGroup("groupA");
                    }

                    @Override
                    public void event(Event event) throws Exception {
                        threads.add(Thread.currentThread().getName());
                        countDownLatch.await();
                    }
                }, new MailboxListener.GroupMailboxListener() {
                    @Override
                    public Group getDefaultGroup() {
                        return new GenericGroup("groupB");
                    }

                    @Override
                    public void event(Event event) throws Exception {
                        threads.add(Thread.currentThread().getName());
                        countDownLatch.await();
                    }
                }, new MailboxListener.GroupMailboxListener() {
                    @Override
                    public Group getDefaultGroup() {
                        return new GenericGroup("groupC");
                    }

                    @Override
                    public void event(Event event) throws Exception {
                        threads.add(Thread.currentThread().getName());
                        countDownLatch.await();
                    }
                });

                eventBus().dispatch(EVENT, NO_KEYS).subscribeOn(Schedulers.elastic()).subscribe();


                getSpeedProfile().shortWaitCondition().atMost(org.awaitility.Duration.TEN_SECONDS)
                    .untilAsserted(() -> assertThat(threads).hasSize(3));
                assertThat(threads).doesNotHaveDuplicates();
            } finally {
                countDownLatch.countDown();
            }
        }

        @Test
        default void listenersShouldBeAbleToDispatch() {
            AtomicBoolean successfulRetry = new AtomicBoolean(false);
            MailboxListener listener = event -> {
                if (event.getEventId().equals(EVENT_ID)) {
                    eventBus().dispatch(EVENT_2, NO_KEYS).block();
                    successfulRetry.set(true);
                }
            };

            eventBus().initialize(listener, GROUP_A);
            eventBus().dispatch(EVENT, NO_KEYS).block();

            getSpeedProfile().shortWaitCondition().until(successfulRetry::get);
        }

        @Test
        default void initializeShouldThrowWhenCalledTwice() {
            AtomicBoolean successfulRetry = new AtomicBoolean(false);
            MailboxListener listener = event -> {
                if (event.getEventId().equals(EVENT_ID)) {
                    eventBus().dispatch(EVENT_2, NO_KEYS).block();
                    successfulRetry.set(true);
                }
            };

            eventBus().initialize(listener, GROUP_A);

            assertThatThrownBy(() -> eventBus().initialize(listener, GROUP_B))
                .isInstanceOf(GroupsAlreadyRegistered.class);
        }

        @Test
        default void listenerGroupShouldReceiveEvents() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            eventBus().dispatch(EVENT, NO_KEYS).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void groupListenersShouldNotReceiveNoopEvents() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            Username bob = Username.of("bob");
            MailboxListener.Added noopEvent = new MailboxListener.Added(MailboxSession.SessionId.of(18), bob, MailboxPath.forUser(bob, "mailbox"), TestId.of(58), ImmutableSortedMap.of(), Event.EventId.random());
            eventBus().dispatch(noopEvent, NO_KEYS).block();

            verify(listener, after(FIVE_HUNDRED_MS.toMillis()).never())
                .event(any());
        }

        @Test
        default void groupListenersShouldReceiveOnlyHandledEvents() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            eventBus().dispatch(EVENT_UNSUPPORTED_BY_LISTENER, NO_KEYS).block();

            verify(listener, after(FIVE_HUNDRED_MS.toMillis()).never())
                .event(any());
        }

        @Test
        default void dispatchShouldNotThrowWhenAGroupListenerFails() throws Exception {
            MailboxListener listener = newListener();
            doThrow(new RuntimeException()).when(listener).event(any());

            eventBus().initialize(listener, GROUP_A);

            assertThatCode(() -> eventBus().dispatch(EVENT, NO_KEYS).block())
                .doesNotThrowAnyException();
        }

        @Test
        default void eachListenerGroupShouldReceiveEvents() throws Exception {
            MailboxListener listener = newListener();
            MailboxListener listener2 = newListener();
            eventBus().initialize(ImmutableMap.of(GROUP_A, listener, GROUP_B, listener2));

            eventBus().dispatch(EVENT, NO_KEYS).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
            verify(listener2, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void dispatchShouldCallSynchronousListener() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            eventBus().dispatch(EVENT, NO_KEYS).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void failingGroupListenersShouldNotAbortGroupDelivery() {
            EventBusTestFixture.MailboxListenerCountingSuccessfulExecution listener = new EventBusTestFixture.EventMatcherThrowingListener(ImmutableSet.of(EVENT));
            eventBus().initialize(listener, GROUP_A);

            eventBus().dispatch(EVENT, NO_KEYS).block();
            eventBus().dispatch(EVENT_2, NO_KEYS).block();

            getSpeedProfile().shortWaitCondition()
                .untilAsserted(() -> assertThat(listener.numberOfEventCalls()).isEqualTo(1));
        }

        @Test
        default void allGroupListenersShouldBeExecutedWhenAGroupListenerFails() throws Exception {
            MailboxListener listener = newListener();

            MailboxListener failingListener = mock(MailboxListener.class);
            when(failingListener.getExecutionMode()).thenReturn(MailboxListener.ExecutionMode.SYNCHRONOUS);
            doThrow(new RuntimeException()).when(failingListener).event(any());

            eventBus().initialize(ImmutableMap.of(GROUP_A, failingListener,
                GROUP_B, listener));

            eventBus().dispatch(EVENT, NO_KEYS).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void allGroupListenersShouldBeExecutedWhenGenericGroups() throws Exception {
            MailboxListener listener1 = newListener();
            MailboxListener listener2 = newListener();

            eventBus().initialize(ImmutableMap.of(new GenericGroup("a"), listener1,
                new GenericGroup("b"), listener2));

            eventBus().dispatch(EVENT, NO_KEYS).block();

            verify(listener1, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
            verify(listener2, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void groupListenerShouldReceiveEventWhenRedeliver() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            eventBus().reDeliver(GROUP_A, EVENT).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void redeliverShouldNotThrowWhenAGroupListenerFails() throws Exception {
            MailboxListener listener = newListener();
            doThrow(new RuntimeException()).when(listener).event(any());

            eventBus().initialize(listener, GROUP_A);

            assertThatCode(() -> eventBus().reDeliver(GROUP_A, EVENT).block())
                .doesNotThrowAnyException();
        }

        @Test
        default void redeliverShouldThrowWhenGroupNotRegistered() {
            eventBus().initialize();
            assertThatThrownBy(() -> eventBus().reDeliver(GROUP_A, EVENT).block())
                .isInstanceOf(GroupRegistrationNotFound.class);
        }

        @Test
        default void redeliverShouldOnlySendEventToDefinedGroup() throws Exception {
            MailboxListener listener = newListener();
            MailboxListener listener2 = newListener();
            eventBus().initialize(ImmutableMap.of(GROUP_A, listener,
                GROUP_B, listener2));

            eventBus().reDeliver(GROUP_A, EVENT).block();

            verify(listener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
            verify(listener2, after(FIVE_HUNDRED_MS.toMillis()).never()).event(any());
        }

        @Test
        default void groupListenersShouldNotReceiveNoopRedeliveredEvents() throws Exception {
            MailboxListener listener = newListener();

            eventBus().initialize(listener, GROUP_A);

            Username bob = Username.of("bob");
            MailboxListener.Added noopEvent = new MailboxListener.Added(MailboxSession.SessionId.of(18), bob, MailboxPath.forUser(bob, "mailbox"), TestId.of(58), ImmutableSortedMap.of(), Event.EventId.random());
            eventBus().reDeliver(GROUP_A, noopEvent).block();

            verify(listener, after(FIVE_HUNDRED_MS.toMillis()).never()).event(any());
        }
    }

    interface MultipleEventBusGroupContract extends EventBusContract.MultipleEventBusContract {

        @Test
        default void groupsDefinedOnlyOnSomeNodesShouldBeNotifiedWhenDispatch() throws Exception {
            MailboxListener mailboxListener = newListener();

            eventBus().initialize(mailboxListener, GROUP_A);

            eventBus2().dispatch(EVENT, NO_KEYS).block();

            verify(mailboxListener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void groupsDefinedOnlyOnSomeNodesShouldNotBeNotifiedWhenRedeliver() {
            MailboxListener mailboxListener = newListener();

            eventBus().initialize(mailboxListener, GROUP_A);

            assertThatThrownBy(() -> eventBus2().reDeliver(GROUP_A, EVENT).block())
                .isInstanceOf(GroupRegistrationNotFound.class);
        }

        @Test
        default void groupListenersShouldBeExecutedOnceWhenRedeliverInADistributedEnvironment() throws Exception {
            MailboxListener mailboxListener = newListener();

            eventBus().initialize(mailboxListener, GROUP_A);
            eventBus2().initialize(mailboxListener, GROUP_A);

            eventBus2().reDeliver(GROUP_A, EVENT).block();

            verify(mailboxListener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void groupListenersShouldBeExecutedOnceInAControlledEnvironment() throws Exception {
            MailboxListener mailboxListener = newListener();

            eventBus().initialize(mailboxListener, GROUP_A);
            eventBus2().initialize(mailboxListener, GROUP_A);

            eventBus2().dispatch(EVENT, NO_KEYS).block();

            verify(mailboxListener, timeout(ONE_SECOND.toMillis()).times(1)).event(any());
        }

        @Test
        default void registerShouldNotDispatchPastEventsForGroupsInADistributedContext() throws Exception {
            MailboxListener listener = newListener();

            eventBus().dispatch(EVENT, NO_KEYS).block();

            eventBus2().initialize(listener, GROUP_A);

            verify(listener, after(FIVE_HUNDRED_MS.toMillis()).never())
                .event(any());
        }
    }
}
