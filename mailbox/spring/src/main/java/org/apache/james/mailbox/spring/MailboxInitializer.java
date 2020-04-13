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

package org.apache.james.mailbox.spring;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.SessionProvider;
import org.apache.james.mailbox.events.EventBusSupplier;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.event.MailboxAnnotationListener;
import org.apache.james.mailbox.store.quota.ListeningCurrentQuotaUpdater;
import org.apache.james.mailbox.store.quota.QuotaUpdater;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;

import com.google.common.collect.ImmutableList;

public class MailboxInitializer {
    private final SessionProvider sessionProvider;
    private final EventBusSupplier eventBus;
    private final MessageSearchIndex messageSearchIndex;
    private final QuotaUpdater quotaUpdater;
    private final MailboxManager mailboxManager;
    private final MailboxSessionMapperFactory mapperFactory;

    @Inject
    public MailboxInitializer(SessionProvider sessionProvider, EventBusSupplier eventBus, MessageSearchIndex messageSearchIndex, QuotaUpdater quotaUpdater, MailboxManager mailboxManager, MailboxSessionMapperFactory mapperFactory) {
        this.sessionProvider = sessionProvider;
        this.eventBus = eventBus;
        this.messageSearchIndex = messageSearchIndex;
        this.quotaUpdater = quotaUpdater;
        this.mailboxManager = mailboxManager;
        this.mapperFactory = mapperFactory;
    }

    public void init() {
        ImmutableList.Builder<MailboxListener.GroupMailboxListener> registrationBuilder = ImmutableList.<MailboxListener.GroupMailboxListener>builder();
        if (messageSearchIndex instanceof ListeningMessageSearchIndex) {
            ListeningMessageSearchIndex index = (ListeningMessageSearchIndex) messageSearchIndex;
            registrationBuilder.add(index);
        }

        if (quotaUpdater instanceof ListeningCurrentQuotaUpdater) {
            ListeningCurrentQuotaUpdater listeningCurrentQuotaUpdater = (ListeningCurrentQuotaUpdater) quotaUpdater;
            registrationBuilder.add(listeningCurrentQuotaUpdater);
        }

        if (mailboxManager.getSupportedMailboxCapabilities().contains(MailboxManager.MailboxCapabilities.Annotation)) {
            registrationBuilder.add(new MailboxAnnotationListener(mapperFactory, sessionProvider));
        }
        eventBus.initialize(registrationBuilder.build());
    }
}
