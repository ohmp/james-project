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

import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.Subscriber;
import org.apache.james.mailbox.events.Group;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

public class UnregisterRemovedGroupsSubscriber implements Subscriber {
    @FunctionalInterface
    public interface Unregisterer {
        Publisher<Void> unregister(Group group);
    }

    private final Unregisterer unregisterer;

    public UnregisterRemovedGroupsSubscriber(Unregisterer unregisterer) {
        this.unregisterer = unregisterer;
    }

    @Override
    public void handle(Event event) {
        if (event instanceof RegisteredGroupListenerChangeEvent) {
            RegisteredGroupListenerChangeEvent changeEvent = (RegisteredGroupListenerChangeEvent) event;

            Flux.fromIterable(changeEvent.getRemovedGroups())
                .concatMap(unregisterer::unregister)
                .then()
                .block();
        }
    }
}
