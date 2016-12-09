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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageManager.FlagsUpdateMode;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.OverQuotaException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.quota.QuotaImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;

public abstract class AbstractMessageIdManagerSideEffectTest {

    private static final Quota OVER_QUOTA = QuotaImpl.quota(102, 100);
    public static final Flags FLAGS = new Flags();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MessageIdManager messageIdManager;
    private MailboxEventDispatcher dispatcher;
    private MailboxSession session;
    private Mailbox mailbox1;
    private Mailbox mailbox2;
    private QuotaManager quotaManager;
    private MessageIdManagerTestingData testingData;

    protected abstract MessageIdManagerTestingData createTestingData(QuotaManager quotaManager, MailboxEventDispatcher dispatcher);

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

    @After
    public void tearDown() {
        testingData.clean();
    }

    @Test
    public void deleteShouldCallEventDispatcher() throws Exception {
        MessageId messageId = testingData.persist(mailbox1.getMailboxId(), FLAGS);
        reset(dispatcher);

        messageIdManager.delete(messageId, ImmutableList.of(mailbox1.getMailboxId()), session);

        verify(dispatcher).expunged(eq(session), any(MessageMetaData.class), eq(mailbox1));
        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void deleteShouldNotCallEventDispatcherWhenMessageIsInWrongMailbox() throws Exception {
        MessageId messageId = testingData.persist(mailbox2.getMailboxId(), FLAGS);
        reset(dispatcher);

        messageIdManager.delete(messageId, ImmutableList.of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setInMailboxesShouldNotCallDispatcherWhenMessageAlreadyInMailbox() throws Exception {
        MessageId messageId = testingData.persist(mailbox1.getMailboxId(), FLAGS);
        reset(dispatcher);

        messageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setInMailboxesShouldCallDispatcher() throws Exception {
        MessageId messageId = testingData.persist(mailbox2.getMailboxId(), FLAGS);
        reset(dispatcher);

        messageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);

        verify(dispatcher).added(eq(session), any(MessageMetaData.class), any(Mailbox.class));
        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setInMailboxesThrowWhenOverQuota() throws Exception {
        MessageId messageId = testingData.persist(mailbox2.getMailboxId(), FLAGS);
        reset(dispatcher);
        when(quotaManager.getMessageQuota(any(QuotaRoot.class))).thenReturn(OVER_QUOTA);

        expectedException.expect(OverQuotaException.class);

        messageIdManager.setInMailboxes(messageId, ImmutableList.<MailboxId>of(mailbox1.getMailboxId()), session);
    }

    @Test
    public void setFlagsShouldNotDispatchWhenFlagAlreadySet() throws Exception {
        Flags newFlags = new Flags(Flags.Flag.SEEN);
        MessageId messageId = testingData.persist(mailbox2.getMailboxId(), newFlags);
        reset(dispatcher);

        messageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId, session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setFlagsShouldDispatch() throws Exception {
        MessageId messageId = testingData.persist(mailbox2.getMailboxId(), FLAGS);
        reset(dispatcher);

        Flags newFlags = new Flags(Flags.Flag.SEEN);
        messageIdManager.setFlags(newFlags, MessageManager.FlagsUpdateMode.ADD, messageId, session);

        List<MessageResult> messages = messageIdManager.getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, session);
        assertThat(messages).hasSize(1);
        MessageResult messageResult = messages.get(0);
        MessageUid messageUid = messageResult.getUid();
        long modSeq = messageResult.getModSeq();
        UpdatedFlags updatedFlags = new UpdatedFlags(messageUid, modSeq, FLAGS, newFlags);

        verify(dispatcher).flagsUpdated(session, messageUid, mailbox1, updatedFlags);
        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void deleteShouldNotDispatchEventWhenMessageDoesNotExist() throws Exception {
        MessageId messageId = testingData.createNotUsedMessageId();

        messageIdManager.delete(messageId, ImmutableList.of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setFlagsShouldNotDispatchEventWhenMessageDoesNotExist() throws Exception {
        MessageId messageId = testingData.createNotUsedMessageId();

        messageIdManager.setFlags(FLAGS, FlagsUpdateMode.ADD, messageId, session);

        verifyNoMoreInteractions(dispatcher);
    }

    @Test
    public void setInMailboxesShouldNotDispatchEventWhenMessageDoesNotExist() throws Exception {
        MessageId messageId = testingData.createNotUsedMessageId();

        messageIdManager.setInMailboxes(messageId, ImmutableList.of(mailbox1.getMailboxId()), session);

        verifyNoMoreInteractions(dispatcher);
    }
}
