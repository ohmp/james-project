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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.CassandraMailQueueViewModule.SizeTable;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Assignment;

class SizeDao {

    private final CassandraAsyncExecutor executor;
    private final PreparedStatement select;
    private final PreparedStatement increment;
    private final PreparedStatement decrement;

    @Inject
    SizeDao(Session session) {
        executor = new CassandraAsyncExecutor(session);
        select = prepareSelect(session);
        increment = prepareIncrement(session, incr(SizeTable.SIZE));
        decrement = prepareIncrement(session, decr(SizeTable.SIZE));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(SizeTable.TABLE_NAME)
            .where(eq(SizeTable.QUEUE_NAME, bindMarker(SizeTable.QUEUE_NAME))));
    }

    private PreparedStatement prepareIncrement(Session session, Assignment assignment) {
        return session.prepare(update(SizeTable.TABLE_NAME)
            .where(eq(SizeTable.QUEUE_NAME, bindMarker(SizeTable.QUEUE_NAME)))
            .with(assignment));
    }

    CompletableFuture<Void> increment(MailQueueName mailQueueName) {
        return executor.executeVoid(increment.bind()
            .setString(SizeTable.QUEUE_NAME, mailQueueName.asString()));
    }

    CompletableFuture<Void> decrement(MailQueueName mailQueueName) {
        return executor.executeVoid(decrement.bind()
            .setString(SizeTable.QUEUE_NAME, mailQueueName.asString()));
    }

    CompletableFuture<Optional<Long>> getSize(MailQueueName mailQueueName) {
        return executor.executeSingleRow(select.bind()
            .setString(SizeTable.QUEUE_NAME, mailQueueName.asString()))
            .thenApply(maybeRow -> maybeRow.map(row -> row.getLong(SizeTable.SIZE)));
    }
}
