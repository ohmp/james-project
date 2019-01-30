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

package org.apache.james.jmap.model;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.core.User;
import org.apache.james.core.quota.QuotaValue;
import org.apache.james.jmap.model.mailbox.Quotas;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.QuotaManager;
import org.apache.james.mailbox.quota.UserQuotaRootResolver;

public class QuotaLoader {
    private final QuotaManager quotaManager;
    private final UserQuotaRootResolver quotaRootResolver;

    @Inject
    public QuotaLoader(QuotaManager quotaManager, UserQuotaRootResolver quotaRootResolver) {
        this.quotaManager = quotaManager;
        this.quotaRootResolver = quotaRootResolver;
    }

    public Quotas getQuotas(MailboxPath mailboxPath) throws MailboxException {
        return getQuotas(quotaRootResolver.getQuotaRoot(mailboxPath));
    }

    public Quotas getQuotas(User user) throws MailboxException {
        return getQuotas(quotaRootResolver.forUser(user));
    }

    public Quotas getQuotas(QuotaRoot quotaRoot) throws MailboxException {
        return Quotas.from(
            Quotas.QuotaId.fromQuotaRoot(quotaRoot),
            Quotas.Quota.from(
                quotaToValue(quotaManager.getStorageQuota(quotaRoot)),
                quotaToValue(quotaManager.getMessageQuota(quotaRoot))));
    }

    private <T extends QuotaValue<T>> Quotas.Value<T> quotaToValue(Quota<T> quota) {
        return new Quotas.Value<>(
            quotaValueToNumber(quota.getUsed()),
            quotaValueToOptionalNumber(quota.getLimit()));
    }

    private Number quotaValueToNumber(QuotaValue<?> value) {
        return Number.BOUND_SANITIZING_FACTORY.from(value.asLong());
    }

    private Optional<Number> quotaValueToOptionalNumber(QuotaValue<?> value) {
        if (value.isUnlimited()) {
            return Optional.empty();
        }
        return Optional.of(quotaValueToNumber(value));
    }

}
