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

package org.apache.james.mailbox.quota.mailing.listeners;

import static org.apache.james.mailbox.quota.model.QuotaThresholdFixture._50;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.apache.james.core.User;
import org.apache.james.eventsourcing.EventStore;
import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.QuotaRoot;
import org.apache.james.mailbox.quota.mailing.QuotaMailingListenerConfiguration;
import org.apache.james.mailbox.quota.model.QuotaThresholdFixture.Quotas.Counts;
import org.apache.james.mailbox.quota.model.QuotaThresholdFixture.Quotas.Sizes;
import org.apache.james.mailbox.quota.model.QuotaThresholds;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMailContext;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface QuotaMailingListenersIntegrationTest {

    Duration GRACE_PERIOD = Duration.ofDays(1);
    QuotaThresholds SINGLE_THRESHOLD = new QuotaThresholds(ImmutableList.of(_50));
    QuotaMailingListenerConfiguration DEFAULT_CONFIGURATION = new QuotaMailingListenerConfiguration(SINGLE_THRESHOLD, GRACE_PERIOD);
    String BOB = "bob@domain";
    MockMailboxSession BOB_SESSION = new MockMailboxSession(BOB);
    User BOB_USER = User.fromUsername(BOB);
    Instant BASE_INSTANT = Instant.now();
    Clock FIXED_CLOCK = Clock.fixed(BASE_INSTANT, ZoneId.systemDefault());
    QuotaRoot QUOTAROOT = QuotaRoot.quotaRoot("any", Optional.empty());

    @Test
    default void shouldNotSendMailWhenUnderAllThresholds(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._30_PERCENT, BASE_INSTANT));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdate(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._55_PERCENT, BASE_INSTANT.minus(Duration.ofHours(1))));

        mailetContext.resetSentMails();

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._55_PERCENT, BASE_INSTANT));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecently(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._55_PERCENT, BASE_INSTANT.minus(Duration.ofHours(12))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._30_PERCENT, BASE_INSTANT.minus(Duration.ofHours(6))));

        mailetContext.resetSentMails();

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._55_PERCENT, BASE_INSTANT));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }


    @Test
    default void shouldSendMailWhenThresholdOverPassed(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(BOB_SESSION, QUOTAROOT, Counts._40_PERCENT, Sizes._55_PERCENT, BASE_INSTANT));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    /*
    @Test
    default void shouldNotSendDuplicates(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotifySeparatelyCountAndSize(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(60))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(2);
    }

    @Test
    default void shouldGroupSizeAndCountNotificationsWhenTriggeredByASingleEvent(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(52))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriod(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(12))));
        store.persistQuotaSizeThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldNotSendMailWhenNoThresholdUpdateForCount(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        store.épersistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(2))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldNotSendMailWhenThresholdOverPassedRecentlyForCount(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofHours(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofHours(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).isEmpty();
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedForCount(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldSendMailWhenThresholdOverPassedOverGracePeriodForCount(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, DEFAULT_CONFIGURATION);

        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(_50,
                BASE_INSTANT.minus(Duration.ofDays(12))));
        store.persistQuotaCountThresholdChange(BOB_USER,
            new QuotaThresholdChange(QuotaThreshold.ZERO,
                BASE_INSTANT.minus(Duration.ofDays(6))));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails()).hasSize(1);
    }

    @Test
    default void shouldUpdateSizeChangesWhenOverPassingLimit(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(
                new QuotaThresholdChange(
                    _50,
                    BASE_INSTANT)));
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChanges(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void shouldNotUpdateSizeChangesWhenNoChange(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);


        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange));
    }

    @Test
    default void shouldUpdateSizeChangesWhenBelow(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAbove(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateSizeChangesWhenAboveButRecentlyOverpasses(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofHours(6)));
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaSizeThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(55))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaSizeThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange1, oldChange2,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenOverPassingLimit(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(
                new QuotaThresholdChange(
                    _50,
                    BASE_INSTANT)));
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChanges(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory());
    }

    @Test
    default void shouldNotUpdateCountChangesWhenNoChange(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange));
    }

    @Test
    default void shouldUpdateCountChangesWhenBelow(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(40))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(30))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(QuotaThreshold.ZERO, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenAbove(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofDays(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldUpdateCountChangesWhenAboveButRecentlyOverpasses(EventStore store) throws Exception {
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext(), store, DEFAULT_CONFIGURATION, FIXED_CLOCK);

        QuotaThresholdChange oldChange1 = new QuotaThresholdChange(_50,
            BASE_INSTANT.minus(Duration.ofHours(12)));
        QuotaThresholdChange oldChange2 = new QuotaThresholdChange(QuotaThreshold.ZERO,
            BASE_INSTANT.minus(Duration.ofHours(6)));
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange1);
        store.persistQuotaCountThresholdChange(BOB_USER, oldChange2);

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER))
            .isEqualTo(new QuotaThresholdHistory(oldChange1, oldChange2,
                new QuotaThresholdChange(_50, BASE_INSTANT)));
    }

    @Test
    default void shouldSendOneNoticePerThreshold(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(55))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(40))
                .computedLimit(QuotaSize.size(100))
                .build()));
        testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
            QuotaRoot.quotaRoot("any", Optional.empty()),
            Quota.<QuotaCount>builder()
                .used(QuotaCount.count(85))
                .computedLimit(QuotaCount.count(100))
                .build(),
            Quota.<QuotaSize>builder()
                .used(QuotaSize.size(42))
                .computedLimit(QuotaSize.size(100))
                .build()));

        assertThat(mailetContext.getSentMails())
            .hasSize(2);
    }

    @Disabled
    @Test
    default void shouldSendOneMailUponConcurrentEvents(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        new ConcurrentTestRunner(10, 1, (threadNb, step) ->
            testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
                QuotaRoot.quotaRoot("any", Optional.empty()),
                Quota.<QuotaCount>builder()
                    .used(QuotaCount.count(55))
                    .computedLimit(QuotaCount.count(100))
                    .build(),
                Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(40))
                    .computedLimit(QuotaSize.size(100))
                    .build())))
            .run()
            .awaitTermination(1, TimeUnit.MINUTES);

        assertThat(mailetContext.getSentMails())
            .hasSize(1);
    }

    @Disabled
    @Test
    default void storeShouldOnlyStoreOneOverPassedThresholdOnceWhenConcurrentEvents(EventStore store) throws Exception {
        FakeMailContext mailetContext = mailetContext();
        QuotaThresholdListenersTestSystem testee = new QuotaThresholdListenersTestSystem(mailetContext, store, new QuotaMailingListenerConfiguration(new QuotaThresholds(_50, _80), GRACE_PERIOD));

        int threadCount = 5;
        new ConcurrentTestRunner(threadCount, 1, (threadNb, step) ->
            testee.event(new MailboxListener.QuotaUsageUpdatedEvent(new MockMailboxSession(BOB),
                QuotaRoot.quotaRoot("any", Optional.empty()),
                Quota.<QuotaCount>builder()
                    .used(QuotaCount.count(60 + threadNb))
                    .computedLimit(QuotaCount.count(100))
                    .build(),
                Quota.<QuotaSize>builder()
                    .used(QuotaSize.size(40))
                    .computedLimit(QuotaSize.size(100))
                    .build())))
            .run()
            .awaitTermination(1, TimeUnit.MINUTES);

        assertThat(store.retrieveQuotaCountThresholdChanges(BOB_USER)
            .getChanges())
            .hasSize(1);
    }
    */

    default FakeMailContext mailetContext() {
        return FakeMailContext.builder()
            .postmaster(MailAddressFixture.POSTMASTER_AT_JAMES)
            .build();
    }

}