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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.apache.james.eventsourcing.eventstore.memory.InMemoryEventStore;
import org.apache.james.mailbox.events.GenericGroup;
import org.apache.james.mailbox.events.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Mono;

class GroupUnregistringManagerTest {
    private static final GenericGroup GROUP_A = new GenericGroup("a");
    private static final GenericGroup GROUP_B = new GenericGroup("b");

    static class TestUnregisterer implements UnregisterRemovedGroupsSubscriber.Unregisterer {
        private ImmutableList.Builder<Group> unregisteredGroups = ImmutableList.builder();

        @Override
        public Publisher<Void> unregister(Group group) {
            return Mono.fromRunnable(() -> unregisteredGroups.add(group));
        }

        ImmutableList<Group> unregisteredGroups() {
            return unregisteredGroups.build();
        }
    }

    GroupUnregistringManager testee;
    TestUnregisterer unregisterer;

    @BeforeEach
    void setUp() {
        unregisterer = new TestUnregisterer();
        testee = new GroupUnregistringManager(new InMemoryEventStore(), unregisterer, Clock.systemUTC());
    }

    @Test
    void startShouldNotUnregisterGroupsWhenNoHistory() {
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .isEmpty();
    }

    @Test
    void startShouldNotUnregisterGroupsWhenNoChanges() {
        testee.start(ImmutableSet.of(GROUP_A)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .isEmpty();
    }

    @Test
    void startShouldNotUnregisterGroupsWhenAdditions() {
        testee.start(ImmutableSet.of(GROUP_A)).block();
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();

        assertThat(unregisterer.unregisteredGroups())
            .isEmpty();
    }

    @Test
    void startShouldUnregisterGroupsWhenRemoval() {
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B);
    }

    @Test
    void startShouldUnregisterGroupsWhenSwap() {
        testee.start(ImmutableSet.of(GROUP_B)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B);
    }

    @Test
    void startShouldBeAbleToUnregisterPreviouslyUnregisteredGroups() {
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B, GROUP_B);
    }

    @Test
    void startWithNoGroupsShouldUnregisterAllPreviousGroups() {
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();
        testee.start(ImmutableSet.of()).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_A, GROUP_B);
    }
}