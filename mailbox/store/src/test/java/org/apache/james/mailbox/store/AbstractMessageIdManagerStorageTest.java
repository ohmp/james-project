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
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

public abstract class AbstractMessageIdManagerStorageTest {

    public static final Flags FLAGS = new Flags();
    private MessageIdManagerTestingData testingData;
    private MessageIdManager messageIdManager;
    private Mailbox mailbox1;
    private Mailbox mailbox2;
    private MailboxSession session;

    protected abstract MessageIdManagerTestingData createTestingData();

    @Before
    public void setUp() {
        testingData = createTestingData();
        messageIdManager = testingData.getMessageIdManager();
        mailbox1 = testingData.getMailbox1();
        mailbox2 = testingData.getMailbox2();

        session = mock(MailboxSession.class);
    }

    @After
    public void tearDown() {
        testingData.clean();
    }

    @Test
    public void getMessagesShouldReturnEmptyListWhenMessageIdNotUsed() throws Exception {
        MessageId messageId = testingData.createNotUsedMessageId();

        assertThat(messageIdManager.getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, session))
            .isEmpty();
    }

    @Test
    public void getMessagesShouldReturnStoredResults() throws Exception {
        MessageId messageId = testingData.persist(mailbox1.getMailboxId(), FLAGS);

        assertThat(messageIdManager.getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, session))
            .hasSize(1);
    }

    @Test
    public void setInMailboxesShouldSetMessageInBothMailboxes() throws Exception {
        MessageId messageId = testingData.persist(mailbox1.getMailboxId(), FLAGS);

        messageIdManager.setInMailboxes(messageId, ImmutableList.of(mailbox2.getMailboxId()), session);

        assertThat(messageIdManager.getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, session))
            .hasSize(2);
    }

    @Test
    public void setInMailboxesShouldNotDuplicateMessageIfSameMailbox() throws Exception {
        MessageId messageId = testingData.persist(mailbox1.getMailboxId(), FLAGS);

        messageIdManager.setInMailboxes(messageId, ImmutableList.of(mailbox1.getMailboxId()), session);

        assertThat(messageIdManager.getMessages(ImmutableList.of(messageId), FetchGroupImpl.MINIMAL, session))
            .hasSize(1);
    }

    @Test
    public void setInMailboxesShouldSetHighestUidInNewMailbox() throws Exception {
        MessageId messageId1 = testingData.persist(mailbox1.getMailboxId(), FLAGS);
        MessageId messageId2 = testingData.persist(mailbox2.getMailboxId(), FLAGS);

        messageIdManager.setInMailboxes(messageId1, ImmutableList.of(mailbox2.getMailboxId()), session);

        MessageUid uidMessage1Mailbox1 = messageIdManager.getMessages(ImmutableList.of(messageId1), FetchGroupImpl.MINIMAL, session)
            .get(0)
            .getUid();
        MessageUid uidMessage2Mailbox1 = FluentIterable
            .from(messageIdManager.getMessages(ImmutableList.of(messageId2), FetchGroupImpl.MINIMAL, session))
            .filter(new Predicate<MessageResult>() {
                @Override
                public boolean apply(MessageResult input) {
                    return input.getMailboxId().equals(mailbox1.getMailboxId());
                }
            })
            .toList()
            .get(0)
            .getUid();

        assertThat(uidMessage2Mailbox1).isGreaterThan(uidMessage1Mailbox1);
    }

    @Test
    public void setInMailboxesShouldSetHighestUidInNewMailbox() throws Exception {
        MessageId messageId1 = testingData.persist(mailbox1.getMailboxId(), FLAGS);
        MessageId messageId2 = testingData.persist(mailbox2.getMailboxId(), FLAGS);

        messageIdManager.setInMailboxes(messageId1, ImmutableList.of(mailbox2.getMailboxId()), session);

        MessageUid uidMessage1Mailbox1 = messageIdManager.getMessages(ImmutableList.of(messageId1), FetchGroupImpl.MINIMAL, session)
            .get(0)
            .getUid();
        long modMessage2Mailbox1 = FluentIterable
            .from(messageIdManager.getMessages(ImmutableList.of(messageId2), FetchGroupImpl.MINIMAL, session))
            .filter(new Predicate<MessageResult>() {
                @Override
                public boolean apply(MessageResult input) {
                    return input.getMailboxId().equals(mailbox1.getMailboxId());
                }
            })
            .toList()
            .get(0)
            .getModSeq();

        assertThat(uidMessage2Mailbox1).isGreaterThan(uidMessage1Mailbox1);
    }




}
