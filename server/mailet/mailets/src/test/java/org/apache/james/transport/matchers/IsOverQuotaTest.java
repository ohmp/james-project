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

package org.apache.james.transport.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.inmemory.quota.InMemoryCurrentQuotaManager;
import org.apache.james.mailbox.inmemory.quota.InMemoryPerUserMaxQuotaManager;
import org.apache.james.mailbox.store.FakeAuthenticator;
import org.apache.james.mailbox.store.FakeAuthorizator;
import org.apache.james.mailbox.store.NoMailboxPathLocker;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.quota.CurrentQuotaCalculator;
import org.apache.james.mailbox.store.quota.DefaultQuotaRootResolver;
import org.apache.james.mailbox.store.quota.QuotaRootImpl;
import org.apache.james.mailbox.store.quota.StoreQuotaManager;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMatcherConfig;
import org.junit.Before;
import org.junit.Test;

public class IsOverQuotaTest {

    private IsOverQuota testee;
    private InMemoryPerUserMaxQuotaManager maxQuotaManager;

    @Before
    public void setUp() throws Exception {
        InMemoryMailboxSessionMapperFactory factory = new InMemoryMailboxSessionMapperFactory();
        InMemoryMailboxManager mailboxManager = new InMemoryMailboxManager(factory, new FakeAuthenticator(), FakeAuthorizator.defaultReject(),
            new NoMailboxPathLocker(), new UnionMailboxACLResolver(), new SimpleGroupMembershipResolver(), new MessageParser(),
            new InMemoryMessageId.Factory());

        DefaultQuotaRootResolver quotaRootResolver = new DefaultQuotaRootResolver(factory);
        StoreQuotaManager quotaManager = new StoreQuotaManager();
        maxQuotaManager = new InMemoryPerUserMaxQuotaManager();
        quotaManager.setMaxQuotaManager(maxQuotaManager);
        quotaManager.setCurrentQuotaManager(new InMemoryCurrentQuotaManager(new CurrentQuotaCalculator(factory, quotaRootResolver), mailboxManager));
        testee = new IsOverQuota(quotaRootResolver, quotaManager, mailboxManager);

        mailboxManager.setQuotaRootResolver(quotaRootResolver);
        mailboxManager.setQuotaManager(quotaManager);
        mailboxManager.init();

        testee.init(FakeMatcherConfig.builder().matcherName("IsOverQuota").build());
    }

    @Test
    public void matchShouldAcceptMailWhenNoQuota() throws Exception {
        FakeMail mail = FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .size(1000)
            .build();

        assertThat(testee.match(mail))
            .isEmpty();
    }

    @Test
    public void matchShouldKeepAddressesWithTooBigSize() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(100);

        FakeMail fakeMail = FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .size(1000)
            .build();

        Collection<MailAddress> result = testee.match(fakeMail);

        assertThat(result).containsOnly(MailAddressFixture.ANY_AT_JAMES);
    }

    @Test
    public void matchShouldReturnEmptyAtSizeQuotaLimit() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(1000);

        FakeMail fakeMail = FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .size(1000)
            .build();

        Collection<MailAddress> result = testee.match(fakeMail);

        assertThat(result).isEmpty();
    }

    @Test
    public void matchShouldKeepAddressesWithTooMuchMessages() throws Exception {
        maxQuotaManager.setDefaultMaxMessage(0);

        FakeMail fakeMail=FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .build();
        Collection <MailAddress> result=testee.match(fakeMail);

        assertThat(result).containsOnly(MailAddressFixture.ANY_AT_JAMES);
    }

    @Test
    public void matchShouldReturnEmptyOnMessageLimit() throws Exception {
        maxQuotaManager.setDefaultMaxMessage(1);

        FakeMail fakeMail=FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .build();
        Collection <MailAddress> result=testee.match(fakeMail);

        assertThat(result).isEmpty();
    }

    @Test
    public void matchShouldNotIncludeRecipientNotOverQuota() throws MessagingException {
        maxQuotaManager.setMaxStorage(QuotaRootImpl.quotaRoot("#private&" + MailAddressFixture.ANY_AT_JAMES.getLocalPart()),
            100);

        FakeMail fakeMail=FakeMail.builder()
            .recipient(MailAddressFixture.ANY_AT_JAMES)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .size(150)
            .build();

        Collection<MailAddress> result = testee.match(fakeMail);
        assertThat(result).containsOnly(MailAddressFixture.ANY_AT_JAMES);
    }

}
