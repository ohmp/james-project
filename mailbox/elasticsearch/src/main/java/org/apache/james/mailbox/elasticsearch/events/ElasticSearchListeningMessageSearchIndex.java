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
package org.apache.james.mailbox.elasticsearch.events;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxManager.MessageCapabilities;
import org.apache.james.mailbox.MailboxManager.SearchCapabilities;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.ListeningMessageSearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ElasticSearchListeningMessageSearchIndex extends ListeningMessageSearchIndex {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchListeningMessageSearchIndex.class);

    private final ElasticSearchSearcher searcher;
    private final ElasticSearchMessageIndexer messageIndexer;

    @Inject
    public ElasticSearchListeningMessageSearchIndex(MessageMapperFactory factory,
                                                    ElasticSearchSearcher searcher,
                                                    ElasticSearchMessageIndexer messageIndexer) {
        super(factory);
        this.searcher = searcher;
        this.messageIndexer = messageIndexer;
    }

    @Override
    public ListenerType getType() {
        return ListenerType.ONCE;
    }

    @Override
    public EnumSet<SearchCapabilities> getSupportedCapabilities(EnumSet<MessageCapabilities> messageCapabilities) {
        EnumSet<SearchCapabilities> supportedCapabilites = EnumSet.of(
            SearchCapabilities.MultimailboxSearch,
            SearchCapabilities.Text,
            SearchCapabilities.FullText,
            SearchCapabilities.PartialEmailMatch);

        if (messageToElasticSearchJson.handleIndexAttachment()) {
            supportedCapabilites.add(SearchCapabilities.Attachment);
        }

        return supportedCapabilites;
    }
    
    @Override
    public Iterator<MessageUid> search(MailboxSession session, Mailbox mailbox, SearchQuery searchQuery) throws MailboxException {
        Preconditions.checkArgument(session != null, "'session' is mandatory");
        Optional<Long> noLimit = Optional.empty();
        return searcher
                .search(ImmutableList.of(mailbox.getMailboxId()), searchQuery, noLimit)
                .map(SearchResult::getMessageUid)
                .iterator();
    }
    
    @Override
    public List<MessageId> search(MailboxSession session, Collection<MailboxId> mailboxIds, SearchQuery searchQuery, long limit)
            throws MailboxException {
        Preconditions.checkArgument(session != null, "'session' is mandatory");

        if (mailboxIds.isEmpty()) {
            return ImmutableList.of();
        }

        return searcher.search(mailboxIds, searchQuery, Optional.empty())
            .peek(this::logIfNoMessageId)
            .map(SearchResult::getMessageId)
            .map(Optional::get)
            .distinct()
            .limit(limit)
            .collect(Guavate.toImmutableList());
    }

    private void logIfNoMessageId(SearchResult searchResult) {
        if (!searchResult.getMessageId().isPresent()) {
            LOGGER.error("No messageUid for {} in mailbox {}", searchResult.getMessageUid(), searchResult.getMailboxId());
        }
    }

    @Override
    public void add(MailboxSession session, Mailbox mailbox, MailboxMessage message) throws MailboxException {
        messageIndexer.add(session, mailbox, message);
    }

    @Override
    public void delete(MailboxSession session, Mailbox mailbox, List<MessageUid> expungedUids) throws MailboxException {
        messageIndexer.delete(session, mailbox, expungedUids);
    }

    @Override
    public void deleteAll(MailboxSession session, Mailbox mailbox) throws MailboxException {
        messageIndexer.deleteAll(session, mailbox);
    }

    @Override
    public void update(MailboxSession session, Mailbox mailbox, List<UpdatedFlags> updatedFlagsList) throws MailboxException {
        messageIndexer.update(session, mailbox, updatedFlagsList);
    }

}
