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

package org.apache.james.mailbox.events.eventsourcing;

import java.time.Clock;

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventSourcingSystem;
import org.apache.james.eventsourcing.Subscriber;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.mailbox.events.Group;
import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GroupUnregistringManager {
    @FunctionalInterface
    public interface Unregisterer {
        Mono<Void> unregister(Group group);
    }

    private class UnregisterRemovedGroupsSubscriber implements Subscriber {
        private final Unregisterer unregisterer;

        public UnregisterRemovedGroupsSubscriber(Unregisterer unregisterer) {
            this.unregisterer = unregisterer;
        }

        @Override
        public void handle(Event event) {
            if (event instanceof RegisteredGroupListenerChangeEvent) {
                RegisteredGroupListenerChangeEvent changeEvent = (RegisteredGroupListenerChangeEvent) event;

                Flux.fromIterable(changeEvent.getRemovedGroups())
                    .concatMap(this::unregister)
                    .then()
                    .block();
            }
        }

        private Publisher<Void> unregister(Group group) {
            return unregisterer.unregister(group)
                .then(notifyUnbind(group));
        }
    }

    private final EventSourcingSystem eventSourcingSystem;

    public GroupUnregistringManager(EventStore eventStore, Unregisterer unregisterer, Clock clock) {
        this.eventSourcingSystem = EventSourcingSystem.fromJava(ImmutableSet.of(
                new RequireGroupsCommandHandler(eventStore, clock),
                new MarkUnbindAsSucceededCommandHandler(eventStore)),
            ImmutableSet.of(new UnregisterRemovedGroupsSubscriber(unregisterer)),
            eventStore);
    }

    public Mono<Void> start(ImmutableSet<Group> groups) {
        RequireGroupsCommand requireGroupsCommand = new RequireGroupsCommand(groups);

        return Mono.from(eventSourcingSystem.dispatch(requireGroupsCommand));
    }

    private Mono<Void> notifyUnbind(Group group) {
        MarkUnbindAsSucceededCommand command = new MarkUnbindAsSucceededCommand(group);

        return Mono.from(eventSourcingSystem.dispatch(command));
    }
}
