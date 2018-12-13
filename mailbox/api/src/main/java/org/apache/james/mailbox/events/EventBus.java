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

import java.util.Set;

import org.apache.james.mailbox.Event;
import org.apache.james.mailbox.MailboxListener;

import com.google.common.collect.ImmutableSet;

import reactor.core.publisher.Mono;

public interface EventBus {
    Registration register(MailboxListener listener, RegistrationKey key);

    Registration register(MailboxListener listener, Group group) throws GroupAlreadyRegistered;

    Mono<Void> dispatch(Event event, Set<RegistrationKey> key);

    default Mono<Void> dispatch(Event event, RegistrationKey key) {
        return dispatch(event, ImmutableSet.of(key));
    }
}
