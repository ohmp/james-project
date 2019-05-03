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

package org.apache.james.queue.rabbitmq.view.cassandra;

import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.BUCKET_ID;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.QUEUE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.SLICE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BucketSizeDAOTest {
    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(CassandraSchemaVersionModule.MODULE, CassandraMailQueueViewModule.MODULE));

    private BucketSizeDAO testee;

    @BeforeEach
    void setUp() {
        testee = new BucketSizeDAO(cassandraCluster.getCassandraCluster().getConf());
    }

    @Test
    void getMailCountShouldReturnZeroWhenNotYetInteractions() {
        Optional<Long> maybeSize = testee.getMailCount(QUEUE_NAME, SLICE, BUCKET_ID).blockOptional();

        assertThat(maybeSize).contains(0L);
    }

    @Test
    void getMailCountShouldReturnIncrementedValue() {
        testee.increment(QUEUE_NAME, SLICE, BUCKET_ID).block();

        Optional<Long> maybeSize = testee.getMailCount(QUEUE_NAME, SLICE, BUCKET_ID).blockOptional();

        assertThat(maybeSize).contains(1L);
    }

    @Test
    void incrementIsNotIdempotent() {
        testee.increment(QUEUE_NAME, SLICE, BUCKET_ID).block();
        testee.increment(QUEUE_NAME, SLICE, BUCKET_ID).block();

        Optional<Long> maybeSize = testee.getMailCount(QUEUE_NAME, SLICE, BUCKET_ID).blockOptional();

        assertThat(maybeSize).contains(2L);
    }

    @Test
    void decrementShouldRevertIncrement() {
        testee.increment(QUEUE_NAME, SLICE, BUCKET_ID).block();
        testee.decrement(QUEUE_NAME, SLICE, BUCKET_ID).block();

        Optional<Long> maybeSize = testee.getMailCount(QUEUE_NAME, SLICE, BUCKET_ID).blockOptional();

        assertThat(maybeSize).contains(0L);
    }

    @Test
    void decrementShouldBeAppliedWhenZero() {
        testee.decrement(QUEUE_NAME, SLICE, BUCKET_ID).block();

        Optional<Long> maybeSize = testee.getMailCount(QUEUE_NAME, SLICE, BUCKET_ID).blockOptional();

        assertThat(maybeSize).contains(-1L);
    }
}