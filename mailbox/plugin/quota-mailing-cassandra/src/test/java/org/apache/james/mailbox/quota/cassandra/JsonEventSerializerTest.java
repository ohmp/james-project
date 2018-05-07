package org.apache.james.mailbox.quota.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.core.User;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.mailing.aggregates.UserQuotaThresholds;
import org.apache.james.mailbox.quota.mailing.events.QuotaThresholdChangedEvent;
import org.apache.james.mailbox.quota.model.HistoryEvolution;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class JsonEventSerializerTest {

    @Test
    void shouldSerializeQuotaThresholdChangedEvent() throws JsonProcessingException {
        String result = new JsonEventSerializer().serialize(
            new QuotaThresholdChangedEvent(
                EventId.first(),
                HistoryEvolution.noChanges(),
                HistoryEvolution.noChanges(),
                Quota.<QuotaSize>builder().used(QuotaSize.size(23)).computedLimit(QuotaSize.size(33)).build(),
                Quota.<QuotaCount>builder().used(QuotaCount.count(12)).computedLimit(QuotaCount.count(45)).build(),
                UserQuotaThresholds.Id.from(User.fromUsername("foo@bar.com"))
            ));
        assertThat(result).isEqualTo("{type: \"quota-threshold-changed\"}");
    }

}