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

import static org.apache.james.mailbox.events.eventsourcing.RegisteredGroupsAggregate.AGGREGATE_ID;

import java.util.Objects;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.mailbox.events.Group;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RegisteredGroupListenerChangeEvent implements Event {

    private final EventId eventId;
    private final ImmutableSet<Group> addedGroups;
    private final ImmutableSet<Group> removedGroups;

    public RegisteredGroupListenerChangeEvent(EventId eventId, ImmutableSet<Group> addedGroups, ImmutableSet<Group> removedGroups) {
        Preconditions.checkArgument(Sets.intersection(addedGroups, removedGroups).isEmpty(),
            "'addedGroups' and 'removedGroups' elements needs to be distinct");

        this.eventId = eventId;
        this.addedGroups = addedGroups;
        this.removedGroups = removedGroups;
    }


    @Override
    public EventId eventId() {
        return eventId;
    }

    @Override
    public AggregateId getAggregateId() {
        return AGGREGATE_ID;
    }

    public ImmutableSet<Group> getAddedGroups() {
        return addedGroups;
    }

    public ImmutableSet<Group> getRemovedGroups() {
        return removedGroups;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof RegisteredGroupListenerChangeEvent) {
            RegisteredGroupListenerChangeEvent that = (RegisteredGroupListenerChangeEvent) o;

            return Objects.equals(this.eventId, that.eventId)
                && Objects.equals(this.addedGroups, that.addedGroups)
                && Objects.equals(this.removedGroups, that.removedGroups);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(eventId, addedGroups, removedGroups);
    }
}
