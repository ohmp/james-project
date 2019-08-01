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
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.BUCKET_ID_2;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.ENQUEUE_ID_1;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.ENQUEUE_ID_2;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.QUEUE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.SLICE;
import static org.apache.james.queue.rabbitmq.view.cassandra.MailQueueViewFixture.SLICE_2;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class EnqueueIdToBucketDAOTest {
    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(CassandraSchemaVersionModule.MODULE, CassandraMailQueueViewModule.MODULE));

    private EnqueueIdToBucketDAO testee;

    @BeforeEach
    void setUp() {
        testee = new EnqueueIdToBucketDAO(cassandraCluster.getCassandraCluster().getConf());
    }

    @Test
    void retrieveBucketShouldReturnEmptyWhenNoCorrespondingEntry() {
        Optional<Pair<BucketedSlices.Slice, BucketedSlices.BucketId>> maybeBucketAndSlice =
            testee.retrieveBucket(QUEUE_NAME, ENQUEUE_ID_1).blockOptional();

        assertThat(maybeBucketAndSlice).isEmpty();
    }

    @Test
    void retrieveBucketShouldReturnRegisteredSliceAndBucket() {
        testee.registerBucket(QUEUE_NAME, ENQUEUE_ID_1, SLICE, BUCKET_ID).block();

        Optional<Pair<BucketedSlices.Slice, BucketedSlices.BucketId>> maybeBucketAndSlice =
            testee.retrieveBucket(QUEUE_NAME, ENQUEUE_ID_1).blockOptional();

        assertThat(maybeBucketAndSlice).contains(Pair.of(SLICE, BUCKET_ID));
    }

    @Test
    void registerBucketShouldOverridePreviouslyStoredData() {
        testee.registerBucket(QUEUE_NAME, ENQUEUE_ID_1, SLICE, BUCKET_ID).block();
        testee.registerBucket(QUEUE_NAME, ENQUEUE_ID_1, SLICE_2, BUCKET_ID_2).block();

        Optional<Pair<BucketedSlices.Slice, BucketedSlices.BucketId>> maybeBucketAndSlice =
            testee.retrieveBucket(QUEUE_NAME, ENQUEUE_ID_1).blockOptional();

        assertThat(maybeBucketAndSlice).contains(Pair.of(SLICE_2, BUCKET_ID_2));
    }

    @Test
    void dataShouldBeIsolatedByEnqueueId() {
        testee.registerBucket(QUEUE_NAME, ENQUEUE_ID_1, SLICE, BUCKET_ID).block();
        testee.registerBucket(QUEUE_NAME, ENQUEUE_ID_2, SLICE_2, BUCKET_ID_2).block();

        Optional<Pair<BucketedSlices.Slice, BucketedSlices.BucketId>> maybeBucketAndSlice =
            testee.retrieveBucket(QUEUE_NAME, ENQUEUE_ID_1).blockOptional();

        assertThat(maybeBucketAndSlice).contains(Pair.of(SLICE, BUCKET_ID));
    }
}