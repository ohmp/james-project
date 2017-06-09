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

import java.io.IOException;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.lucene.search.LuceneMessageSearchIndex;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MultimailboxesSearchQuery;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.apache.james.mailbox.store.search.SimpleMessageSearchIndex;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

public class LuceneSearchMailboxModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SimpleMessageSearchIndex.class).in(Scopes.SINGLETON);
        bind(MessageSearchIndex.class).to(SimpleMessageSearchIndex.class);
        bind(ListeningMessageSearchIndex.class).toInstance(new ListeningMessageSearchIndex() {
            @Override
            public void add(MailboxSession session, Mailbox mailbox, MailboxMessage message) throws MailboxException {

            }

            @Override
            public void delete(MailboxSession session, Mailbox mailbox, List<MessageUid> expungedUids) throws MailboxException {

            }

            @Override
            public void deleteAll(MailboxSession session, Mailbox mailbox) throws MailboxException {

            }

            @Override
            public void update(MailboxSession session, Mailbox mailbox, List<UpdatedFlags> updatedFlagsList) throws MailboxException {

            }

            @Override
            public ListenerType getType() {
                return ListenerType.EACH_NODE;
            }

            @Override
            public Iterator<MessageUid> search(MailboxSession session, Mailbox mailbox, SearchQuery searchQuery) throws MailboxException {
                return null;
            }

            @Override
            public List<MessageId> search(MailboxSession session, MultimailboxesSearchQuery searchQuery, long limit) throws MailboxException {
                return null;
            }

            @Override
            public EnumSet<MailboxManager.SearchCapabilities> getSupportedCapabilities() {
                return null;
            }
        });
    }

    @Provides
    @Singleton
    Directory provideDirectory(FileSystem fileSystem) throws IOException {
        return FSDirectory.open(fileSystem.getBasedir());
    }
}
