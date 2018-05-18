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
package org.apache.james.mailbox.quota.mailing.search;

import org.apache.james.core.Domain;
import org.apache.james.mailbox.quota.model.QuotaThreshold;

import com.google.common.base.Preconditions;

public interface QuotaQuery {

    public static class LessThanQuery implements QuotaQuery {

        private final QuotaThreshold quotaThreshold;

        public LessThanQuery(QuotaThreshold quotaThreshold) {
            Preconditions.checkNotNull(quotaThreshold, "'quotaThreshold' is mandatory");
            this.quotaThreshold = quotaThreshold;
        }

        public QuotaThreshold getQuotaThreshold() {
            return quotaThreshold;
        }
    }

    public static class MoreThanQuery implements QuotaQuery {

        private final QuotaThreshold quotaThreshold;

        public MoreThanQuery(QuotaThreshold quotaThreshold) {
            Preconditions.checkNotNull(quotaThreshold, "'quotaThreshold' is mandatory");
            this.quotaThreshold = quotaThreshold;
        }

        public QuotaThreshold getQuotaThreshold() {
            return quotaThreshold;
        }
    }

    public static class HasDomainQuery implements QuotaQuery {

        private final Domain domain;

        public HasDomainQuery(Domain domain) {
            Preconditions.checkNotNull(domain, "'domain' is mandatory");
            this.domain = domain;
        }

        public Domain getDomain() {
            return domain;
        }
    }

    public static class AndQuery implements QuotaQuery {

        private final QuotaQuery left;
        private final QuotaQuery right;

        public AndQuery(QuotaQuery left, QuotaQuery right) {
            Preconditions.checkNotNull(left, "'left' is mandatory");
            Preconditions.checkNotNull(right, "'right' is mandatory");
            this.left = left;
            this.right = right;
        }

        public QuotaQuery getLeft() {
            return left;
        }

        public QuotaQuery getRight() {
            return right;
        }
    }

    public static class OrQuery implements QuotaQuery {

        private final QuotaQuery left;
        private final QuotaQuery right;

        public OrQuery(QuotaQuery left, QuotaQuery right) {
            Preconditions.checkNotNull(left, "'left' is mandatory");
            Preconditions.checkNotNull(right, "'right' is mandatory");
            this.left = left;
            this.right = right;
        }

        public QuotaQuery getLeft() {
            return left;
        }

        public QuotaQuery getRight() {
            return right;
        }
    }
}
