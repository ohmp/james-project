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
import static com.datastax.driver.core.querybuilder.QueryBuilder.decr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.incr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.BucketSizeTable.BUCKET_ID;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.BucketSizeTable.MAIL_COUNT;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.BucketSizeTable.QUEUE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.BucketSizeTable.TABLE_NAME;
import static org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.BucketSizeTable.TIME_RANGE_START;

import java.util.Date;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Assignment;

import reactor.core.publisher.Mono;

public class BucketSizeDAO {
    private final CassandraAsyncExecutor executor;
    private final PreparedStatement increment;
    private final PreparedStatement decrement;
    private final PreparedStatement select;

    @Inject
    BucketSizeDAO(Session session) {
        this.executor = new CassandraAsyncExecutor(session);

        this.increment = prepareUpdate(session, incr(MAIL_COUNT));
        this.decrement = prepareUpdate(session, decr(MAIL_COUNT));
        this.select = prepareSelect(session);
    }

    private PreparedStatement prepareUpdate(Session session, Assignment assignment) {
        return session.prepare(
            update(TABLE_NAME)
                .with(assignment)
                .where(eq(QUEUE_NAME, bindMarker(QUEUE_NAME)))
                .and(eq(TIME_RANGE_START, bindMarker(TIME_RANGE_START)))
                .and(eq(BUCKET_ID, bindMarker(BUCKET_ID))));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(
            select(MAIL_COUNT)
                .from(TABLE_NAME)
                .where(eq(QUEUE_NAME, bindMarker(QUEUE_NAME)))
                .and(eq(TIME_RANGE_START, bindMarker(TIME_RANGE_START)))
                .and(eq(BUCKET_ID, bindMarker(BUCKET_ID))));
    }

    Mono<Long> getMailCount(MailQueueName queueName, BucketedSlices.Slice slice, BucketedSlices.BucketId bucketId) {
        return executor.executeSingleRowOptional(
            select.bind()
                .setString(QUEUE_NAME, queueName.asString())
                .setTimestamp(TIME_RANGE_START, Date.from(slice.getStartSliceInstant()))
                .setInt(BUCKET_ID, bucketId.getValue()))
            .map(maybeRow -> maybeRow.map(row -> row.getLong(MAIL_COUNT)))
            .map(maybeCount -> maybeCount.orElse(0L));
    }

    Mono<Void> increment(MailQueueName queueName, BucketedSlices.Slice slice, BucketedSlices.BucketId bucketId) {
        return executor.executeVoid(
            increment.bind()
                .setString(QUEUE_NAME, queueName.asString())
                .setTimestamp(TIME_RANGE_START, Date.from(slice.getStartSliceInstant()))
                .setInt(BUCKET_ID, bucketId.getValue()));
    }

    Mono<Void> decrement(MailQueueName queueName, BucketedSlices.Slice slice, BucketedSlices.BucketId bucketId) {
        return executor.executeVoid(
            decrement.bind()
                .setString(QUEUE_NAME, queueName.asString())
                .setTimestamp(TIME_RANGE_START, Date.from(slice.getStartSliceInstant()))
                .setInt(BUCKET_ID, bucketId.getValue()));
    }
}
