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

import org.apache.james.eventsourcing.EventSourcingSystem;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.mailbox.events.Group;

import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Mono;

public class GroupUnregistringManager {
    private final EventSourcingSystem eventSourcingSystem;

    public GroupUnregistringManager(EventStore eventStore, UnregisterRemovedGroupsSubscriber.Unregisterer unregisterer) {
        this.eventSourcingSystem = EventSourcingSystem.fromJava(ImmutableSet.of(new StartCommandHandler(eventStore)),
            ImmutableSet.of(new UnregisterRemovedGroupsSubscriber(unregisterer)),
            eventStore);
    }

    public Mono<Void> start(ImmutableSet<Group> groups) {
        StartCommand startCommand = new StartCommand(groups);

        return Mono.from(eventSourcingSystem.dispatch(startCommand));
    }
}
