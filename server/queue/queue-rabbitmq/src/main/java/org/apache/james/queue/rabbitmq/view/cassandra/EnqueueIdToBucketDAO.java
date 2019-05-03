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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueueIdToBucketTable.BUCKET_ID;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueueIdToBucketTable.ENQUEUE_ID;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueueIdToBucketTable.QUEUE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueueIdToBucketTable.TABLE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.EnqueueIdToBucketTable.TIME_RANGE_START;

import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.queue.rabbitmq.EnqueueId;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.Slice;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import reactor.core.publisher.Mono;

class EnqueueIdToBucketDAO {
    private final CassandraAsyncExecutor executor;
    private final PreparedStatement selectOne;
    private final PreparedStatement insertOne;

    @Inject
    EnqueueIdToBucketDAO(Session session) {
        this.executor = new CassandraAsyncExecutor(session);

        this.selectOne = prepareSelect(session);
        this.insertOne = prepareInsert(session);
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(QUEUE_NAME, bindMarker(QUEUE_NAME))
            .value(ENQUEUE_ID, bindMarker(ENQUEUE_ID))
            .value(TIME_RANGE_START, bindMarker(TIME_RANGE_START))
            .value(BUCKET_ID, bindMarker(BUCKET_ID)));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(TABLE_NAME)
            .where(eq(QUEUE_NAME, bindMarker(QUEUE_NAME)))
            .and(eq(ENQUEUE_ID, bindMarker(ENQUEUE_ID))));
    }

    Mono<Void> registerBucket(MailQueueName mailQueueName, EnqueueId enqueueId, Slice slice, BucketId bucketId) {
        return executor.executeVoid(insertOne.bind()
            .setString(QUEUE_NAME, mailQueueName.asString())
            .setUUID(ENQUEUE_ID, enqueueId.asUUID())
            .setTimestamp(TIME_RANGE_START, Date.from(slice.getStartSliceInstant()))
            .setInt(BUCKET_ID, bucketId.getValue()));
    }

    Mono<Pair<Slice, BucketId>> retrieveBucket(MailQueueName mailQueueName, EnqueueId enqueueId) {
        return executor.executeSingleRowOptional(
            selectOne.bind()
                .setString(QUEUE_NAME, mailQueueName.asString())
                .setUUID(ENQUEUE_ID, enqueueId.asUUID()))
            .flatMap(maybeRow -> Mono.justOrEmpty(maybeRow.map(row -> Pair.of(
                Slice.of(row.getTimestamp(TIME_RANGE_START).toInstant()),
                BucketId.of(row.getInt(BUCKET_ID))))));
    }
}
