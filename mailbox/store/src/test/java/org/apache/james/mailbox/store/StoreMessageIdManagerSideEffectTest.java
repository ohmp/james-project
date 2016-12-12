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
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.junit.Before;

import com.google.common.collect.ImmutableList;

public class StoreMessageIdManagerSideEffectTest extends AbstractMessageIdManagerSideEffectTest {
    /**
     * 
     */
    private static final TestId MAILBOX_ID3 = TestId.of(56);
    /**
     * 
     */
    private static final TestId MAILBOX_ID2 = TestId.of(46);
    /**
     * 
     */
    private static final TestId MAILBOX_ID1 = TestId.of(36);
    private final static long UID_VALIDITY = 42;
    private static final long MOD_SEQ = 18;
    private static final MessageUid UID = MessageUid.of(28);
    private static final ByteArrayInputStream ARRAY_INPUT_STREAM = new ByteArrayInputStream("".getBytes());
    private StoreMessageIdManager messageIdManager;
    private DefaultMessageId.Factory messageIdFactory = new DefaultMessageId.Factory();
    private MailboxSession session;
    private MailboxPath mailboxPath1 = new MailboxPath("#private", "user", "INBOX");
    private MailboxPath mailboxPath2 = new MailboxPath("#private", "user", "OUTBOX");
    private MailboxPath mailboxPath3 = new MailboxPath("#private", "user", "SENT");

    private Mailbox mailbox1;
    private Mailbox mailbox2;
    private Mailbox mailbox3;
    private MessageIdMapper messageIdMapper;
    private MailboxMapper mailboxMapper;
    private MailboxSessionMapperFactory mailboxSessionMapperFactory;
    private MessageId messageId;
    
    @Before
    public void setUp() throws Exception {
        messageId = messageIdFactory.generate();
        messageIdMapper = mock(MessageIdMapper.class);
        mailboxMapper = mock(MailboxMapper.class);
        ModSeqProvider modSeqProvider = mock(ModSeqProvider.class);
        UidProvider uidProvider = mock(UidProvider.class);

        mailboxSessionMapperFactory = mock(MailboxSessionMapperFactory.class);
        when(mailboxSessionMapperFactory.getMessageIdMapper(any(MailboxSession.class))).thenReturn(messageIdMapper);
        when(mailboxSessionMapperFactory.getMailboxMapper(any(MailboxSession.class))).thenReturn(mailboxMapper);
        
        session = mock(MailboxSession.class);

        mailbox1 = new SimpleMailbox(mailboxPath1, UID_VALIDITY, MAILBOX_ID1);
        mailbox2 = new SimpleMailbox(mailboxPath1, UID_VALIDITY, MAILBOX_ID2);
        mailbox3 = new SimpleMailbox(mailboxPath1, UID_VALIDITY, MAILBOX_ID3);

        when(mailboxMapper.findMailboxById(MAILBOX_ID1)).thenReturn(mailbox1);
        when(mailboxMapper.findMailboxById(MAILBOX_ID2)).thenReturn(mailbox2);
        when(mailboxMapper.findMailboxById(MAILBOX_ID3)).thenReturn(mailbox3);

        MailboxMessage mailboxMessage1 = createMessage(MAILBOX_ID1);
        MailboxMessage mailboxMessage2 = createMessage(MAILBOX_ID2);
        MailboxMessage mailboxMessage3 = createMessage(MAILBOX_ID3);

        when(messageIdMapper.find(ImmutableList.of(messageId), MessageMapper.FetchType.Full)).thenReturn(ImmutableList.of(mailboxMessage1, mailboxMessage3));
        when(mailboxSessionMapperFactory.getModSeqProvider()).thenReturn(modSeqProvider);
        when(mailboxSessionMapperFactory.getUidProvider()).thenReturn(uidProvider);
        when(uidProvider.nextUid(any(MailboxSession.class), any(MailboxId.class))).thenReturn(UID);
        when(modSeqProvider.nextModSeq(any(MailboxSession.class), any(MailboxId.class))).thenReturn(MOD_SEQ);
        
        super.setUp();
    }

    @Override
    protected MessageIdManagerTestSystem createTestingData(QuotaManager quotaManager,
            MailboxEventDispatcher dispatcher) throws Exception {
        messageIdManager = new StoreMessageIdManager(mailboxSessionMapperFactory, dispatcher, messageIdFactory,
                quotaManager, new DefaultQuotaRootResolver(mailboxSessionMapperFactory));

        return new MessageIdManagerTestSystem(messageIdManager, mailbox1, mailbox2, mailbox3) {
            
            @Override
            public MessageId persist(MailboxId mailboxId, Flags flags) {
                return messageId;
            }
            
            @Override
            public void deleteMailbox(MailboxId mailboxId) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public MessageId createNotUsedMessageId() {
                return messageIdFactory.generate();
            }
            
            @Override
            public void clean() {
                // TODO Auto-generated method stub
                
            }
        };
    }

    private MailboxMessage createMessage(MailboxId mailboxId) throws IOException {
        MailboxMessage mailboxMessage = mock(MailboxMessage.class);
        when(mailboxMessage.getUid()).thenReturn(UID);
        when(mailboxMessage.getMailboxId()).thenReturn(mailboxId);
        when(mailboxMessage.getFullContent()).thenReturn(ARRAY_INPUT_STREAM);
        when(mailboxMessage.createFlags()).thenReturn(FLAGS);
        return mailboxMessage;
    }

}
