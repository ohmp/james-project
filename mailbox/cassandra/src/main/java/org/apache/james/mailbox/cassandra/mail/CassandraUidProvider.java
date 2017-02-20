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

package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageUidTable.NEXT_UID;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraMessageUidTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.util.OptionalConverter;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;

public class CassandraUidProvider implements UidProvider {
    public final static int DEFAULT_MAX_RETRY = 100000;

    private final Session session;
    private final CassandraAsyncExecutor executor;
    private final FunctionRunnerWithRetry runner;

    public CassandraUidProvider(Session session, int maxRetry) {
        this.session = session;
        this.executor = new CassandraAsyncExecutor(session);
        this.runner = new FunctionRunnerWithRetry(maxRetry);
    }

    @Inject
    public CassandraUidProvider(Session session) {
        this(session, DEFAULT_MAX_RETRY);
    }

    @Override
    public MessageUid nextUid(MailboxSession mailboxSession, Mailbox mailbox) throws MailboxException {
        return nextUid(mailboxSession, mailbox.getMailboxId());
    }

    @Override
    public MessageUid nextUid(MailboxSession session, MailboxId mailboxId) throws MailboxException {
        CassandraId cassandraId = (CassandraId) mailboxId;
        return nextUid(cassandraId)
        .join()
        .orElseThrow(() -> new MailboxException("Error during Uid update"));
    }

    public CompletableFuture<Optional<MessageUid>> nextUid(CassandraId cassandraId) {
        return findHighestUid(cassandraId)
            .thenCompose(optional -> {
                if (optional.isPresent()) {
                    return tryUpdateUid(cassandraId, optional);
                }
                return tryInsert(cassandraId);
            })
            .thenCompose(optional -> {
                if (optional.isPresent()) {
                    return CompletableFuture.completedFuture(optional);
                }
                return runner.executeAsyncAndRetieveObject(
                    () -> findHighestUid(cassandraId)
                        .thenCompose(readUid -> tryUpdateUid(cassandraId, readUid)));
            });
    }

    @Override
    public com.google.common.base.Optional<MessageUid> lastUid(MailboxSession mailboxSession, Mailbox mailbox) throws MailboxException {
        return OptionalConverter.toGuava(findHighestUid((CassandraId) mailbox.getMailboxId()).join());
    }

    private CompletableFuture<Optional<MessageUid>> findHighestUid(CassandraId mailboxId) {
        return executor.executeSingleRow(
            select(NEXT_UID)
                .from(CassandraMessageUidTable.TABLE_NAME)
                .where(eq(CassandraMessageUidTable.MAILBOX_ID, mailboxId.asUuid())))
            .thenApply(optional -> optional.map(row -> MessageUid.of(row.getLong(NEXT_UID))));
    }

    private CompletableFuture<Optional<MessageUid>> tryUpdateUid(CassandraId mailboxId, Optional<MessageUid> uid) {
        if (uid.isPresent()) {
            MessageUid nextUid = uid.get().next();
            return executor.executeReturnApplied(
                update(CassandraMessageUidTable.TABLE_NAME)
                    .onlyIf(eq(NEXT_UID, uid.get().asLong()))
                    .with(set(NEXT_UID, nextUid.asLong()))
                    .where(eq(CassandraMessageUidTable.MAILBOX_ID, mailboxId.asUuid())))
                .thenApply(success -> successToUid(nextUid, success));
        } else {
            return tryInsert(mailboxId);
        }
    }

    private CompletableFuture<Optional<MessageUid>> tryInsert(CassandraId mailboxId) {
        return executor.executeReturnApplied(
            insertInto(CassandraMessageUidTable.TABLE_NAME)
                .value(NEXT_UID, MessageUid.MIN_VALUE.asLong())
                .value(CassandraMessageUidTable.MAILBOX_ID, mailboxId.asUuid())
                .ifNotExists())
            .thenApply(success -> successToUid(MessageUid.MIN_VALUE, success));
    }

    private Optional<MessageUid> successToUid(MessageUid uid, Boolean success) {
        if (success) {
            return Optional.of(uid);
        }
        return Optional.empty();
    }

    private Optional<MessageUid> transactionalStatementToOptionalUid(MessageUid uid, BuiltStatement statement) {
        if(session.execute(statement).one().getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED)) {
            return Optional.of(uid);
        }
        return Optional.empty();
    }
}
