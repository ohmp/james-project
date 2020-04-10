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
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;
import org.apache.james.mailbox.events.Group;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RegisteredGroupsAggregate {
    static AggregateId AGGREGATE_ID = () -> "RegisteredGroupListenerChangeEvent";

    private static class State {
        static State initial() {
            return new State(ImmutableSet.of());
        }

        final ImmutableSet<Group> groups;

        private State(ImmutableSet<Group> groups) {
            this.groups = groups;
        }

        private State apply(RegisteredGroupListenerChangeEvent event) {
            return new State(ImmutableSet.<Group>builder()
                .addAll(Sets.difference(groups, event.getRemovedGroups()))
                .addAll(event.getAddedGroups())
                .build());
        }
    }

    public static RegisteredGroupsAggregate load(History history) {
        return new RegisteredGroupsAggregate(history);
    }

    private final History history;
    private State state;

    private RegisteredGroupsAggregate(History history) {
        this.history = history;
        this.state = State.initial();

        history.getEventsJava()
            .forEach(this::apply);
    }

    public List<RegisteredGroupListenerChangeEvent> handle(RequireGroupsCommand requireGroupsCommand, Clock clock) {
        List<RegisteredGroupListenerChangeEvent> detectedChanges = detectChanges(requireGroupsCommand, clock);

        detectedChanges.forEach(this::apply);

        return detectedChanges;
    }

    private List<RegisteredGroupListenerChangeEvent> detectChanges(RequireGroupsCommand requireGroupsCommand, Clock clock) {
        ImmutableSet<Group> addedGroups = ImmutableSet.copyOf(Sets.difference(requireGroupsCommand.getRegisteredGroups(), state.groups));
        ImmutableSet<Group> removedGroups = ImmutableSet.copyOf(Sets.difference(state.groups, requireGroupsCommand.getRegisteredGroups()));

        if (!addedGroups.isEmpty() || !removedGroups.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.ofInstant(clock.instant(), clock.getZone());
            RegisteredGroupListenerChangeEvent event = new RegisteredGroupListenerChangeEvent(history.getNextEventId(),
                Hostname.localHost(),
                now,
                addedGroups,
                removedGroups);
            return ImmutableList.of(event);
        }
        return ImmutableList.of();
    }

    private void apply(Event event) {
        Preconditions.checkArgument(event instanceof RegisteredGroupListenerChangeEvent);

        state = state.apply((RegisteredGroupListenerChangeEvent) event);
    }
}
