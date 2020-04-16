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
import java.util.List;

import org.apache.james.eventsourcing.eventstore.memory.InMemoryEventStore;
import org.apache.james.mailbox.events.GenericGroup;
import org.apache.james.mailbox.events.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class GroupUnregistringManagerTest {
    private static final GenericGroup GROUP_A = new GenericGroup("a");
    private static final GenericGroup GROUP_B = new GenericGroup("b");
    private TestRegisteredGroupsProvider registeredGroupsProvider;

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

    static class TestRegisteredGroupsProvider implements UnregisterRemovedGroupsSubscriber.RegisteredGroupsProvider {
        private List<Group> groups = ImmutableList.of();

        void setRegisteredGroups(ImmutableList<Group> registeredGroups) {
            groups = registeredGroups;
        }

        @Override
        public Publisher<Group> registeredGroups() {
            return Flux.fromIterable(groups);
        }
    }

    GroupUnregistringManager testee;
    TestUnregisterer unregisterer;

    @BeforeEach
    void setUp() {
        unregisterer = new TestUnregisterer();
        registeredGroupsProvider = new TestRegisteredGroupsProvider();
        testee = new GroupUnregistringManager(new InMemoryEventStore(), unregisterer, registeredGroupsProvider, Clock.systemUTC());
    }

    @Test
    void startShouldPersistRequiredGroup() {
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(testee.requiredGroups().block())
            .containsOnly(GROUP_A);
    }

    @Test
    void startShouldOverwritePreviousStart() {
        testee.start(ImmutableSet.of(GROUP_A)).block();

        testee.start(ImmutableSet.of(GROUP_B)).block();

        assertThat(testee.requiredGroups().block())
            .containsOnly(GROUP_B);
    }

    @Test
    void startShouldBeAbleToAddAGroup() {
        testee.start(ImmutableSet.of(GROUP_A)).block();

        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();

        assertThat(testee.requiredGroups().block())
            .containsOnly(GROUP_A, GROUP_B);
    }

    @Test
    void startShouldBeAbleToRemoveAGroup() {
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();

        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(testee.requiredGroups().block())
            .containsOnly(GROUP_A);
    }

    @Test
    void startShouldBeAbleToRemoveAllGroups() {
        testee.start(ImmutableSet.of(GROUP_A, GROUP_B)).block();

        testee.start(ImmutableSet.of()).block();

        assertThat(testee.requiredGroups().block())
            .isEmpty();
    }

    @Test
    void requiredGroupsShouldReturnEmptyByDefault() {
        assertThat(testee.requiredGroups().block())
            .isEmpty();
    }

    @Test
    void startShouldUnregisterGroupsWhenRegisteredButNotRequired() {
        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A, GROUP_B));

        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B);
    }

    @Test
    void startShouldNotUnregisterGroupsWhenRegisteredAndRequired() {
        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A));

        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .isEmpty();
    }

    @Test
    void startShouldNotUnregisterGroupsWhenNotRegisteredButRequired() {
        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A, GROUP_B));

        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B);
    }

    @Test
    void startShouldUnregisterGroupsTwiceWhenGroupStillRegistered() {
        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A, GROUP_B));

        testee.start(ImmutableSet.of(GROUP_A)).block();
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B, GROUP_B);
    }

    @Test
    void startShouldUnregisterGroupsOnceWhenGroupNoLongerRegistered() {
        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A, GROUP_B));
        testee.start(ImmutableSet.of(GROUP_A)).block();

        registeredGroupsProvider.setRegisteredGroups(ImmutableList.of(GROUP_A));
        testee.start(ImmutableSet.of(GROUP_A)).block();

        assertThat(unregisterer.unregisteredGroups())
            .containsExactly(GROUP_B);
    }
}