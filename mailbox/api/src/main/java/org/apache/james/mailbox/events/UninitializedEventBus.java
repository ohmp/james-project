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

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public interface UninitializedEventBus {
    EventBus initialize(Map<Group, MailboxListener> listeners) throws GroupsAlreadyRegistered;

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default EventBus initialize(MailboxListener listener, Group group) throws GroupsAlreadyRegistered {
        return initialize(ImmutableMap.of(group, listener));
    }

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default EventBus initialize(MailboxListener.GroupMailboxListener... groupMailboxListeners) throws GroupsAlreadyRegistered {
        return initialize(ImmutableList.copyOf(groupMailboxListeners));
    }

    /**
     * @throws GroupsAlreadyRegistered when any initialize method had already been called
     */
    default EventBus initialize(Collection<MailboxListener.GroupMailboxListener> groupMailboxListeners) throws GroupsAlreadyRegistered {
        Map<Group, MailboxListener> collect = groupMailboxListeners.stream()
            .collect(Guavate.toImmutableMap(
                MailboxListener.GroupMailboxListener::getDefaultGroup,
                MailboxListener.class::cast));
        return initialize(collect);
    }
}
