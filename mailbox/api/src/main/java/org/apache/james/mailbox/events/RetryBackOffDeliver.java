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

import static org.apache.james.mailbox.events.EventBusConstants.ErrorHandling.DEFAULT_JITTER_FACTOR;
import static org.apache.james.mailbox.events.EventBusConstants.ErrorHandling.FIRST_BACKOFF;
import static org.apache.james.mailbox.events.EventBusConstants.ErrorHandling.MAX_BACKOFF;
import static org.apache.james.mailbox.events.EventBusConstants.ErrorHandling.MAX_RETRIES;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

public class RetryBackOffDeliver {

    @FunctionalInterface
    public interface DeliverOperation {
        void deliver(MailboxListener mailboxListener, Event event);
    }

    @FunctionalInterface
    public interface RequireEvent {
        RequireOperation event(Event event);
    }

    @FunctionalInterface
    public interface RequireOperation {
        ReadyToDeliver deliverOperation(DeliverOperation deliverOperation);
    }

    public static RequireEvent mailboxListener(MailboxListener mailboxListener) {
        return event -> operation -> new ReadyToDeliver(mailboxListener, event, operation);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryBackOffDeliver.class);

    public static class ReadyToDeliver {
        private final MailboxListener mailboxListener;
        private final Event event;
        private final DeliverOperation operation;

        private ReadyToDeliver(MailboxListener mailboxListener, Event event, DeliverOperation operation) {
            this.mailboxListener = mailboxListener;
            this.event = event;
            this.operation = operation;
        }

        public Mono<Void> deliver() {
            return Mono.fromRunnable(() -> operation.deliver(mailboxListener, event))
                .doOnError(throwable -> LOGGER.error("Error while processing listener {} for {}",
                    listenerName(mailboxListener),
                    eventName(event),
                    throwable))
                .retryBackoff(MAX_RETRIES, FIRST_BACKOFF, MAX_BACKOFF, DEFAULT_JITTER_FACTOR)
                .doOnError(throwable -> LOGGER.error("listener {} exceeded maximum retry({}) to handle event {}",
                    listenerName(mailboxListener),
                    MAX_RETRIES,
                    eventName(event),
                    throwable))
                .then();
        }
    }

    private static String listenerName(MailboxListener mailboxListener) {
        return mailboxListener.getClass().getCanonicalName();
    }

    private static String eventName(Event event) {
        return event.getClass().getCanonicalName();
    }
}
