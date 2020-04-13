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
package org.apache.james.modules.mailbox;

import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.mailbox.events.EventBus;
import org.apache.james.mailbox.events.EventBusSupplier;
import org.apache.james.mailbox.events.GenericGroup;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.utils.ClassName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class MailboxListenersLoaderImpl implements Configurable, MailboxListenersLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailboxListenersLoaderImpl.class);

    private final MailboxListenerFactory mailboxListenerFactory;
    private final EventBusSupplier eventBus;
    private final Set<MailboxListener.GroupMailboxListener> guiceDefinedListeners;

    @Inject
    MailboxListenersLoaderImpl(MailboxListenerFactory mailboxListenerFactory, EventBusSupplier eventBus,
                               Set<MailboxListener.GroupMailboxListener> guiceDefinedListeners) {
        this.mailboxListenerFactory = mailboxListenerFactory;
        this.eventBus = eventBus;
        this.guiceDefinedListeners = guiceDefinedListeners;
    }

    @Override
    public void configure(HierarchicalConfiguration<ImmutableNode> configuration) {
        configure(ListenersConfiguration.from(configuration));
    }

    public EventBus configure(ListenersConfiguration listenersConfiguration) {
        LOGGER.info("Loading user registered mailbox listeners");

        ImmutableMap<Group, MailboxListener.GroupMailboxListener> guiceListenersAsMap = guiceDefinedListeners.stream()
            .collect(Guavate.toImmutableMap(MailboxListener.GroupMailboxListener::getDefaultGroup));

        ImmutableMap<Group, MailboxListener> registeredListenersAsMap = listenersConfiguration.getListenersConfiguration()
            .stream()
            .map(this::createListener)
            .collect(Guavate.toImmutableMap(
                Pair::getLeft,
                Pair::getRight));

        return register(ImmutableMap.<Group, MailboxListener>builder()
            .putAll(guiceListenersAsMap)
            .putAll(registeredListenersAsMap)
            .build());
    }

    @Override
    public EventBus register(Map<Group, MailboxListener> listeners) {
        return eventBus.initialize(listeners);
    }

    @Override
    public Pair<Group, MailboxListener> createListener(ListenerConfiguration configuration) {
        ClassName listenerClass = new ClassName(configuration.getClazz());
        try {
            LOGGER.info("Loading user registered mailbox listener {}", listenerClass);
            MailboxListener mailboxListener = mailboxListenerFactory.newInstance()
                .withConfiguration(configuration.getConfiguration())
                .withExecutionMode(configuration.isAsync().map(this::getExecutionMode))
                .clazz(listenerClass)
                .build();


            return configuration.getGroup()
                .map(GenericGroup::new)
                .map(group -> Pair.<Group, MailboxListener>of(group, mailboxListener))
                .orElseGet(() -> withDefaultGroup(mailboxListener));
        } catch (ClassNotFoundException e) {
            LOGGER.error("Error while loading user registered global listener {}", listenerClass, e);
            throw new RuntimeException(e);
        }
    }

    private Pair<Group, MailboxListener> withDefaultGroup(MailboxListener mailboxListener) {
        Preconditions.checkArgument(mailboxListener instanceof MailboxListener.GroupMailboxListener);

        MailboxListener.GroupMailboxListener groupMailboxListener = (MailboxListener.GroupMailboxListener) mailboxListener;
        return Pair.of(groupMailboxListener.getDefaultGroup(), groupMailboxListener);
    }

    private MailboxListener.ExecutionMode getExecutionMode(boolean isAsync) {
        if (isAsync) {
            return MailboxListener.ExecutionMode.ASYNCHRONOUS;
        }
        return MailboxListener.ExecutionMode.SYNCHRONOUS;
    }
}
