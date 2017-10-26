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
package org.apache.james.mailbox.store.search;

import java.util.Optional;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.event.EventFactory;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MessageSearchIndex} which needs to get registered as global {@link MailboxListener} and so get
 * notified about message changes. This will then allow to update the underlying index.
 * 
 *
 */
public class ListeningIndexerDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListeningIndexerDelegate.class);

    public static final int UNLIMITED = -1;
    private final MessageIndexer messageIndexer;
    private final MessageMapperFactory factory;

    public ListeningIndexerDelegate(MessageIndexer messageIndexer, MessageMapperFactory factory) {
        this.messageIndexer = messageIndexer;
        this.factory = factory;
    }

    /**
     * Process the {@link Event} and update the index if
     * something relevant is received
     */
    public void event(MailboxListener.Event event) {
        MailboxSession session = event.getSession();

        try {
            if (event instanceof MailboxListener.MessageEvent) {
                if (event instanceof EventFactory.AddedImpl) {
                    EventFactory.AddedImpl added = (EventFactory.AddedImpl) event;
                    Mailbox mailbox = added.getMailbox();

                    for (final MessageUid next : added.getUids()) {
                        Optional<MailboxMessage> mailboxMessage = retrieveMailboxMessage(session, added, mailbox, next);
                        mailboxMessage.ifPresent(message -> addMessage(session, mailbox, message));
                    }
                } else if (event instanceof EventFactory.ExpungedImpl) {
                    EventFactory.ExpungedImpl expunged = (EventFactory.ExpungedImpl) event;
                    try {
                        messageIndexer.delete(session, expunged.getMailbox(), expunged.getUids());
                    } catch (MailboxException e) {
                        LOGGER.error("Unable to deleted messages " + expunged.getUids() + " from index for mailbox " + expunged.getMailbox(), e);
                    }
                } else if (event instanceof EventFactory.FlagsUpdatedImpl) {
                    EventFactory.FlagsUpdatedImpl flagsUpdated = (EventFactory.FlagsUpdatedImpl) event;
                    Mailbox mailbox = flagsUpdated.getMailbox();

                    try {
                        messageIndexer.update(session, mailbox, flagsUpdated.getUpdatedFlags());
                    } catch (MailboxException e) {
                        LOGGER.error("Unable to update flags in index for mailbox " + mailbox, e);
                    }
                }
            } else if (event instanceof EventFactory.MailboxDeletionImpl) {
                messageIndexer.deleteAll(session, ((EventFactory.MailboxDeletionImpl) event).getMailbox());
            }
        } catch (MailboxException e) {
            LOGGER.error("Unable to update index", e);
        }
    }

    private Optional<MailboxMessage> retrieveMailboxMessage(MailboxSession session, EventFactory.AddedImpl added, Mailbox mailbox, MessageUid next) {
        Optional<MailboxMessage> firstChoice = Optional.ofNullable(added.getAvailableMessages().get(next));
        if (firstChoice.isPresent()) {
            return firstChoice;
        } else {
            try {
                return Optional.of(factory.getMessageMapper(session)
                    .findInMailbox(mailbox, MessageRange.one(next), FetchType.Full, UNLIMITED)
                    .next());
            } catch (Exception e) {
                LOGGER.error(String.format("Could not retrieve message %d in mailbox %s",
                    next, mailbox.getMailboxId().serialize()), e);
                return Optional.empty();
            }
        }
    }

    private void addMessage(final MailboxSession session, final Mailbox mailbox, MailboxMessage message) {
        try {
            messageIndexer.add(session, mailbox, message);
        } catch (MailboxException e) {
            LOGGER.error("Unable to index message " + message.getUid() + " for mailbox " + mailbox, e);
        }
    }
}
