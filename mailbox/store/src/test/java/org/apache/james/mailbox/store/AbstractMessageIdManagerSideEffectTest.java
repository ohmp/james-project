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

package org.apache.james.mailbox.store;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.OverQuotaException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.quota.QuotaImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public abstract class AbstractMessageIdManagerSideEffectTest {

    private TestingData testingData;

    private abstract class TestingData {
        private final MessageIdManager messageIdManager;
        private final Mailbox mailbox1;
        private final Mailbox mailbox2;

        public TestingData(MessageIdManager messageIdManager, Mailbox mailbox1, Mailbox mailbox2) {
            this.messageIdManager = messageIdManager;
            this.mailbox1 = mailbox1;
            this.mailbox2 = mailbox2;
        }

        public MessageIdManager getMessageIdManager() {
            return messageIdManager;
        }

        public Mailbox getMailbox1() {
            return mailbox1;
        }

        public Mailbox getMailbox2() {
            return mailbox2;
        }

        // Should take care of find returning the MailboxMessage
        // Should take care of findMailboxes returning the mailbox the message is in
        // Should persist flags // Should keep track of flag state for setFlags
        public abstract void persist(MailboxMessage mailboxMessage);
    }

    private static final MessageUid UID = MessageUid.of(28);
    private static final long MOD_SEQ = 18;
    private static final Quota OVER_QUOTA = QuotaImpl.quota(102, 100);
    public static final Flags FLAGS = new Flags();
    private static final ByteArrayInputStream ARRAY_INPUT_STREAM = new ByteArrayInputStream("".getBytes());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MessageIdManager messageIdManager;
    private MailboxEventDispatcher dispatcher;
    private MailboxSession session;
    private Mailbox mailbox1;
    private Mailbox mailbox2;
    private QuotaManager quotaManager;

    protected abstract TestingData createTestingData(QuotaManager quotaManager, MailboxEventDispatcher dispatcher);

    @Before
    public void setUp() throws Exception {
        dispatcher = mock(MailboxEventDispatcher.class);
        session = mock(MailboxSession.class);
        quotaManager = mock(QuotaManager.class);

        testingData = createTestingData(quotaManager, dispatcher);
        messageIdManager = testingData.getMessageIdManager();
        mailbox1 = testingData.getMailbox1();
        mailbox2 = testingData.getMailbox2();

        when(quotaManager.getMessageQuota(any(QuotaRoot.class))).thenReturn(QuotaImpl.unlimited());
        when(quotaManager.getStorageQuota(any(QuotaRoot.class))).thenReturn(QuotaImpl.unlimited());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteShouldCallEventDispatcher() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox1.getMailboxId(), FLAGS);

        testingData.persist(mailboxMessage);

        messageIdManager.delete(mailboxMessage.getMessageId(), ImmutableList.of(mailbox1.getMailboxId()), session);

        verify(dispatcher).expunged(eq(session), any(MessageMetaData.class), eq(mailbox1));
        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deleteShouldNotCallEventDispatcherWhenMessageIsInWrongMailbox() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox2.getMailboxId(), FLAGS);


        testingData.persist(mailboxMessage);

        messageIdManager.delete(mailboxMessage.getMessageId(), ImmutableList.of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesShouldNotCallDispatcherWhenMessageAlreadyInMailbox() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox1.getMailboxId(), FLAGS);

        testingData.persist(mailboxMessage);

        messageIdManager.setInMailboxes(mailboxMessage.getMessageId(), ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesShouldCallDispatcher() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox2.getMailboxId(), FLAGS);

        testingData.persist(mailboxMessage);

        messageIdManager.setInMailboxes(mailboxMessage.getMessageId(), ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);

        verify(dispatcher).added(eq(session), any(MessageMetaData.class), any(Mailbox.class));
        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setInMailboxesThrowWhenOverQuota() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox2.getMailboxId(), FLAGS);
        testingData.persist(mailboxMessage);
        when(quotaManager.getMessageQuota(any(QuotaRoot.class))).thenReturn(OVER_QUOTA);

        expectedException.expect(OverQuotaException.class);

        messageIdManager.setInMailboxes(mailboxMessage.getMessageId(), ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);
    }

    @Test
    public void setFlagsShouldNotDispatchWhenFlagAlreadySet() throws Exception {
        Flags newFlags = new Flags(Flags.Flag.SEEN);
        MailboxMessage mailboxMessage = createMessage(mailbox2.getMailboxId(), newFlags);
        testingData.persist(mailboxMessage);

        messageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, mailboxMessage.getMessageId(), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setFlagsShouldDispatch() throws Exception {
        MailboxMessage mailboxMessage = createMessage(mailbox2.getMailboxId(), FLAGS);
        testingData.persist(mailboxMessage);

        Flags newFlags = new Flags(Flags.Flag.SEEN);
        messageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, mailboxMessage.getMessageId(), session);

        UpdatedFlags updatedFlags = new UpdatedFlags(UID, MOD_SEQ, FLAGS, newFlags);
        verify(dispatcher).flagsUpdated(session, UID, mailbox1, updatedFlags);
        verifyNoMoreInteractions(dispatcher);
    }

    private MailboxMessage createMessage(MailboxId mailboxId, Flags flags) throws IOException {
        MailboxMessage mailboxMessage = mock(MailboxMessage.class);
        when(mailboxMessage.getUid()).thenReturn(UID);
        when(mailboxMessage.getMailboxId()).thenReturn(mailboxId);
        when(mailboxMessage.getFullContent()).thenReturn(ARRAY_INPUT_STREAM);
        when(mailboxMessage.createFlags()).thenReturn(flags);
        return mailboxMessage;
    }
}
