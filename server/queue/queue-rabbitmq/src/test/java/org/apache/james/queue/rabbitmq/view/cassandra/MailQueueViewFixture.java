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

import java.time.Instant;

import org.apache.james.queue.rabbitmq.EnqueueId;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices;

public interface MailQueueViewFixture {
    MailQueueName QUEUE_NAME = MailQueueName.fromString("mq");
    EnqueueId ENQUEUE_ID_1 = EnqueueId.ofSerialized("110e8400-e29b-11d4-a716-446655440000");
    EnqueueId ENQUEUE_ID_2 = EnqueueId.ofSerialized("464765a0-e4e7-11e4-aba4-710c1de3782b");
    Instant SLICE_INSTANT = Instant.parse("2018-05-20T12:00:00.000Z");
    Instant SLICE_INSTANT_2 = Instant.parse("2018-06-20T12:00:00.000Z");
    BucketedSlices.Slice SLICE = BucketedSlices.Slice.of(SLICE_INSTANT);
    BucketedSlices.Slice SLICE_2 = BucketedSlices.Slice.of(SLICE_INSTANT_2);
    BucketedSlices.BucketId BUCKET_ID = BucketedSlices.BucketId.of(2);
    BucketedSlices.BucketId BUCKET_ID_2 = BucketedSlices.BucketId.of(3);
}
