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

package org.apache.james.mailbox.elasticsearch.tasks;

import java.util.Iterator;
import java.util.function.BinaryOperator;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchMessageIndexer;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.consumers.ThrowingConsumer;
import com.google.common.collect.ImmutableList;

public class ElasticSearchReIndexer {

    public static enum ReIndexingResult {
        SUCCESS,
        FAILED
    }

    public static BinaryOperator<ReIndexingResult> resultCombiner = (result1, result2) -> {
        if (result1 == ReIndexingResult.SUCCESS && result2 == ReIndexingResult.SUCCESS) {
            return ReIndexingResult.SUCCESS;
        }
        return ReIndexingResult.FAILED;
    };

    private static final int UNLIMITED = -1;
    public static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchReIndexer.class);
    private final MailboxManager mailboxManager;
    private final MailboxMapperFactory mailboxMapperFactory;
    private final MessageMapperFactory messageMapperFactory;
    private final ElasticSearchMessageIndexer messageIndexer;

    public ElasticSearchReIndexer(MailboxManager mailboxManager,
                                  MailboxMapperFactory mailboxMapperFactory,
                                  MessageMapperFactory messageMapperFactory,
                                  ElasticSearchMessageIndexer messageIndexer) {
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
        this.messageMapperFactory = messageMapperFactory;
        this.messageIndexer = messageIndexer;
    }

    public ReIndexingResult reindexAll() {
        try {
            LOGGER.info("Starting re-indexing all mailboxes");
            ReIndexingResult result = mailboxMapperFactory.getMailboxMapper(getMailboxSession())
                .list()
                .stream()
                .map(Mailbox::getMailboxId)
                .map(this::reIndex)
                .reduce(ReIndexingResult.SUCCESS, ElasticSearchReIndexer.resultCombiner);
            LOGGER.info("Finished re-indexing all mailboxes : " + result);
            return result;
        } catch (MailboxException e) {
            LOGGER.error("Could not index mailboxes", e);
            return ReIndexingResult.FAILED;
        }
    }

    public ReIndexingResult reIndex(MailboxId mailboxId) {
        try {
            LOGGER.info("Starting re-indexing mailbox {}", mailboxId);
            Mailbox mailbox = mailboxMapperFactory.getMailboxMapper(getMailboxSession())
                .findMailboxById(mailboxId);
            ImmutableList<MailboxMessage> messages = ImmutableList.copyOf(messageMapperFactory.getMessageMapper(getMailboxSession())
                .findInMailbox(mailbox, MessageRange.all(), MessageMapper.FetchType.Metadata, UNLIMITED));
            ReIndexingResult result = messages.stream()
                .map(message -> reIndex(mailbox, message.getUid()))
                .reduce(ReIndexingResult.SUCCESS, ElasticSearchReIndexer.resultCombiner);
            LOGGER.info("Finished re-indexing mailbox {} : " + result, mailbox);
            return result;
        } catch (MailboxException e) {
            LOGGER.error("Could not index single mailbox {}", mailboxId, e);
            return ReIndexingResult.FAILED;
        }
    }

    public ReIndexingResult reIndex(MailboxId mailboxId, MessageUid uid) throws MailboxException {
        try {
            Mailbox mailbox = mailboxMapperFactory.getMailboxMapper(getMailboxSession())
                .findMailboxById(mailboxId);
            return reIndex(mailbox, uid);
        } catch (MailboxException e) {
            LOGGER.error("Could not index message {} {}", mailboxId, uid, e);
            return ReIndexingResult.FAILED;
        }
    }

    private ReIndexingResult reIndex(Mailbox mailbox, MessageUid uid) {
        try {
            Iterator<MailboxMessage> messages = messageMapperFactory.getMessageMapper(getMailboxSession())
                .findInMailbox(mailbox, MessageRange.one(uid), MessageMapper.FetchType.Full, UNLIMITED);
            ThrowingConsumer<MailboxMessage> reIndex = (MailboxMessage message) -> messageIndexer.add(getMailboxSession(), mailbox, message);

            ImmutableList.copyOf(messages)
                .forEach(Throwing.consumer(reIndex).sneakyThrow());
            LOGGER.info("Indexing of message {} {} succeeded", mailbox, uid);
            return ReIndexingResult.SUCCESS;
        } catch (MailboxException e) {
            LOGGER.error("Could not index message {} {}", mailbox.getMailboxId(), uid, e);
            return ReIndexingResult.FAILED;
        }
    }

    private MailboxSession getMailboxSession() throws MailboxException {
        return mailboxManager.createSystemSession("elasticSearchReindexing");
    }
}
