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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Mono;

public interface EventBus {
    int EXECUTION_RATE = 10;

    interface StructuredLoggingFields {
        String EVENT_ID = "eventId";
        String EVENT_CLASS = "eventClass";
        String LISTENER_CLASS = "listenerClass";
        String USER = "user";
        String GROUP = "group";
        String REGISTRATION_KEYS = "registrationKeys";
        String REGISTRATION_KEY = "registrationKey";
    }

    interface Metrics {
        static String timerName(MailboxListener mailboxListener) {
            return "mailbox-listener-" + mailboxListener.getClass().getSimpleName();
        }
    }

    Registration register(MailboxListener listener, RegistrationKey key);

    void initialize(Map<Group, MailboxListener> listeners) throws GroupsAlreadyRegistered;

    Mono<Void> dispatch(Event event, Set<RegistrationKey> key);

    Mono<Void> reDeliver(Group group, Event event);

    default Mono<Void> dispatch(Event event, RegistrationKey key) {
        return dispatch(event, ImmutableSet.of(key));
    }

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default void initialize(MailboxListener listener, Group group) throws GroupsAlreadyRegistered {
        initialize(ImmutableMap.of(group, listener));
    }

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default void initialize(MailboxListener.GroupMailboxListener... groupMailboxListeners) throws GroupsAlreadyRegistered {
        initialize(ImmutableList.copyOf(groupMailboxListeners));
    }

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default void initialize(Collection<MailboxListener.GroupMailboxListener> groupMailboxListeners) throws GroupsAlreadyRegistered {
        Map<Group, MailboxListener> collect = groupMailboxListeners.stream()
            .collect(Guavate.toImmutableMap(
                MailboxListener.GroupMailboxListener::getDefaultGroup,
                MailboxListener.class::cast));
        initialize(collect);
    }
}
