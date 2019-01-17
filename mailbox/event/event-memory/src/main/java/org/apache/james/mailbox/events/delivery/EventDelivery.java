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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.events.Group;

import reactor.core.publisher.Mono;

public interface EventDelivery {

    class DeliverableListener {
        public static Stream<DeliverableListener> from(Map<Group, MailboxListener> mailboxListeners) {
            return mailboxListeners.entrySet().stream()
                .map(entry -> DeliverableListener.from(entry.getKey(), entry.getValue()));
        }

        private static DeliverableListener from(Group group, MailboxListener listener) {
            return new DeliverableListener(group, listener);
        }

        static DeliverableListener withoutGroup(MailboxListener listener) {
            return new DeliverableListener(listener);
        }

        private final Optional<Group> group;
        private final MailboxListener mailboxListener;

        private DeliverableListener(MailboxListener mailboxListener) {
            this.group = Optional.empty();
            this.mailboxListener = mailboxListener;
        }

        private DeliverableListener(Group group, MailboxListener mailboxListener) {
            this.group = Optional.ofNullable(group);
            this.mailboxListener = mailboxListener;
        }

        public Optional<Group> getGroup() {
            return group;
        }

        public MailboxListener getMailboxListener() {
            return mailboxListener;
        }
    }

    class ExecutionStages {
        private final Mono<Void> synchronousListenerFuture;
        private final Mono<Void> asynchronousListenerFuture;

        ExecutionStages(Mono<Void> synchronousListenerFuture, Mono<Void> asynchronousListenerFuture) {
            this.synchronousListenerFuture = synchronousListenerFuture;
            this.asynchronousListenerFuture = asynchronousListenerFuture;
        }

        public Mono<Void> synchronousListenerFuture() {
            return synchronousListenerFuture;
        }

        public Mono<Void> allListenerFuture() {
            return synchronousListenerFuture
                .concatWith(asynchronousListenerFuture)
                .then();
        }
    }


    ExecutionStages deliver(Collection<MailboxListener> mailboxListeners, Event event);

    ExecutionStages deliverWithRetries(Collection<InVmEventDelivery.DeliverableListener> deliverableListeners, Event event);
}
