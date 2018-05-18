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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.core.Domain;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.junit.jupiter.api.Test;

public class QuotaQueryTest implements QuotaQuery {

    @Test
    public void lessThanQueryShouldThrowWhenQuotaThresholdIsNull() {
        assertThatThrownBy(() -> new LessThanQuery(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void lessThanQueryShouldInstanciateWhenQuotaThresholdGiven() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.5);

        LessThanQuery lessThanQuery = new LessThanQuery(quotaThreshold);

        assertThat(lessThanQuery.getQuotaThreshold()).isEqualTo(quotaThreshold);
    }

    @Test
    public void moreThanQueryShouldThrowWhenQuotaThresholdIsNull() {
        assertThatThrownBy(() -> new MoreThanQuery(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void moreThanQueryShouldInstanciateWhenQuotaThresholdGiven() {
        QuotaThreshold quotaThreshold = new QuotaThreshold(0.5);

        MoreThanQuery moreThanQuery = new MoreThanQuery(quotaThreshold);

        assertThat(moreThanQuery.getQuotaThreshold()).isEqualTo(quotaThreshold);
    }

    @Test
    public void HasDomainQueryShouldThrowWhenDomainIsNull() {
        assertThatThrownBy(() -> new HasDomainQuery(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void HasDomainQueryShouldInstanciateWhenDomainGiven() {
        Domain domain = Domain.of("domain.org");

        HasDomainQuery hasDomainQuery = new HasDomainQuery(domain);

        assertThat(hasDomainQuery.getDomain()).isEqualTo(domain);
    }

    @Test
    public void AndQueryShouldThrowWhenLeftIsNull() {
        MoreThanQuery right = new MoreThanQuery(new QuotaThreshold(0.5));
        assertThatThrownBy(() -> new AndQuery(null, right))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void AndQueryShouldThrowWhenRightIsNull() {
        MoreThanQuery left = new MoreThanQuery(new QuotaThreshold(0.5));
        assertThatThrownBy(() -> new AndQuery(left, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void AndQueryShouldInstanciateWhenLeftAndRightAreGiven() {
        MoreThanQuery left = new MoreThanQuery(new QuotaThreshold(0.5));
        MoreThanQuery right = new MoreThanQuery(new QuotaThreshold(0.6));

        AndQuery andQuery = new AndQuery(left, right);

        assertThat(andQuery.getLeft()).isEqualTo(left);
        assertThat(andQuery.getRight()).isEqualTo(right);
    }

    @Test
    public void OrQueryShouldThrowWhenLeftIsNull() {
        MoreThanQuery right = new MoreThanQuery(new QuotaThreshold(0.5));
        assertThatThrownBy(() -> new OrQuery(null, right))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void OrQueryShouldThrowWhenRightIsNull() {
        MoreThanQuery left = new MoreThanQuery(new QuotaThreshold(0.5));
        assertThatThrownBy(() -> new OrQuery(left, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void OrQueryShouldInstanciateWhenLeftAndRightAreGiven() {
        MoreThanQuery left = new MoreThanQuery(new QuotaThreshold(0.5));
        MoreThanQuery right = new MoreThanQuery(new QuotaThreshold(0.6));

        OrQuery orQuery = new OrQuery(left, right);

        assertThat(orQuery.getLeft()).isEqualTo(left);
        assertThat(orQuery.getRight()).isEqualTo(right);
    }
}
