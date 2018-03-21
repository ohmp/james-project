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

package org.apache.james.mailbox.jpa.quota;

import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.jpa.quota.model.JpaCurrentQuota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.store.quota.StoreCurrentQuotaManager;

import com.google.common.base.Preconditions;

public class JpaCurrentQuotaManager implements StoreCurrentQuotaManager {

    public static final long NO_MESSAGES = 0L;
    public static final long NO_STORED_BYTES = 0L;

    private final EntityManagerFactory entityManagerFactory;

    @Inject
    public JpaCurrentQuotaManager(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public MailboxListener.ListenerType getAssociatedListenerType() {
        return MailboxListener.ListenerType.ONCE;
    }

    @Override
    public QuotaCount getCurrentMessageCount(QuotaRoot quotaRoot) {
        return Optional.ofNullable(retrieveUserQuota(quotaRoot))
            .map(JpaCurrentQuota::getMessageCount)
            .orElse(QuotaCount.count(NO_STORED_BYTES));
    }

    @Override
    public QuotaSize getCurrentStorage(QuotaRoot quotaRoot) {
        return Optional.ofNullable(retrieveUserQuota(quotaRoot))
            .map(JpaCurrentQuota::getSize)
            .orElse(QuotaSize.size(NO_STORED_BYTES));
    }

    @Override
    public void increase(QuotaRoot quotaRoot, long count, long size) {
        Preconditions.checkArgument(count > 0, "Counts should be positive");
        Preconditions.checkArgument(size > 0, "Size should be positive");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        JpaCurrentQuota jpaCurrentQuota = Optional.ofNullable(retrieveUserQuota(quotaRoot))
            .orElse(new JpaCurrentQuota(quotaRoot.getValue(), NO_MESSAGES, NO_STORED_BYTES));

        entityManager.merge(new JpaCurrentQuota(quotaRoot.getValue(),
            jpaCurrentQuota.getMessageCount().asLong() + count,
            jpaCurrentQuota.getSize().asLong() + size));

        entityManager.getTransaction().commit();
    }

    @Override
    public void decrease(QuotaRoot quotaRoot, long count, long size) throws MailboxException {
        Preconditions.checkArgument(count > 0, "Counts should be positive");
        Preconditions.checkArgument(size > 0, "Counts should be positive");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        JpaCurrentQuota jpaCurrentQuota = Optional.ofNullable(retrieveUserQuota(quotaRoot))
            .orElse(new JpaCurrentQuota(quotaRoot.getValue(), NO_MESSAGES, NO_STORED_BYTES));

        entityManager.merge(new JpaCurrentQuota(quotaRoot.getValue(),
            jpaCurrentQuota.getMessageCount().asLong() - count,
            jpaCurrentQuota.getSize().asLong() - size));

        entityManager.getTransaction().commit();
    }

    private JpaCurrentQuota retrieveUserQuota(QuotaRoot quotaRoot) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return entityManager.find(JpaCurrentQuota.class, quotaRoot.getValue());
    }
}
