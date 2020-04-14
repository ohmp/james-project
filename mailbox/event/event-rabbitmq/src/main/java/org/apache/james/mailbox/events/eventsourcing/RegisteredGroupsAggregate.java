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
import java.util.Map;
import java.util.Set;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;
import org.apache.james.mailbox.events.Group;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RegisteredGroupsAggregate {
    static AggregateId AGGREGATE_ID = () -> "RegisteredGroupListenerChangeEvent";

    enum Status {
        USED,
        UNUSED_BUT_BINDED
    }

    private static class State {
        static State initial() {
            return new State(ImmutableMap.of());
        }

        final ImmutableMap<Group, Status> groups;

        private State(ImmutableMap<Group, Status> groups) {
            this.groups = groups;
        }

        private State apply(RegisteredGroupListenerChangeEvent event) {
            ImmutableMap<Group, Status> removedGroups = event.getRemovedGroups()
                .stream()
                .collect(Guavate.toImmutableMap(Functions.identity(), any -> Status.UNUSED_BUT_BINDED));
            ImmutableMap<Group, Status> addedGroups = event.getAddedGroups()
                .stream()
                .collect(Guavate.toImmutableMap(Functions.identity(), any -> Status.USED));
            ImmutableMap<Group, Status> unchangedGroups = notIn(Sets.union(event.getAddedGroups(), event.getRemovedGroups()));

            return new State(ImmutableMap.<Group, Status>builder()
                .putAll(addedGroups)
                .putAll(unchangedGroups)
                .putAll(removedGroups)
                .build());
        }

        private State apply(UnbindSucceededEvent event) {
            return new State(ImmutableMap.<Group, Status>builder()
                .putAll(notIn(ImmutableSet.of(event.getGroup())))
                .build());
        }

        private ImmutableSet<Group> usedGroups() {
            return groups.entrySet()
                    .stream()
                    .filter(group -> group.getValue() == Status.USED)
                    .map(Map.Entry::getKey)
                    .collect(Guavate.toImmutableSet());
        }

        private ImmutableSet<Group> bindedGroups() {
            return groups.entrySet()
                    .stream()
                    .filter(group -> group.getValue() == Status.UNUSED_BUT_BINDED)
                    .map(Map.Entry::getKey)
                    .collect(Guavate.toImmutableSet());
        }

        private ImmutableMap<Group, Status> notIn(Set<Group> toBeFilteredOut) {
            return groups.entrySet()
                    .stream()
                    .filter(group -> !toBeFilteredOut.contains(group.getKey()))
                    .collect(Guavate.entriesToImmutableMap());
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

    public List<UnbindSucceededEvent> handle(MarkUnbindAsSucceededCommand command) {
        Preconditions.checkArgument(state.bindedGroups().contains(command.getSucceededGroup()),
            "unbing a non binded group, or a used group");

        UnbindSucceededEvent event = new UnbindSucceededEvent(history.getNextEventId(), command.getSucceededGroup());
        apply(event);
        return ImmutableList.of(event);
    }

    private List<RegisteredGroupListenerChangeEvent> detectChanges(RequireGroupsCommand requireGroupsCommand, Clock clock) {
        ImmutableSet<Group> addedGroups = ImmutableSet.copyOf(Sets.difference(requireGroupsCommand.getRegisteredGroups(), state.usedGroups()));
        ImmutableSet<Group> removedGroups = ImmutableSet.<Group>builder()
            .addAll(Sets.difference(state.usedGroups(), requireGroupsCommand.getRegisteredGroups()))
            .addAll(state.bindedGroups())
            .build();

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
        if (event instanceof RegisteredGroupListenerChangeEvent) {
            state = state.apply((RegisteredGroupListenerChangeEvent) event);
        } else if (event instanceof UnbindSucceededEvent) {
            state = state.apply((UnbindSucceededEvent) event);
        } else {
            throw new RuntimeException("Unsupported event class " + event.getClass());
        }
    }
}
