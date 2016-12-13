package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.decr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.incr;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxCountersTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Assignment;

public class CassandraMailboxCountersDAO {

    private final CassandraAsyncExecutor cassandraAsyncExecutor;

    @Inject
    public CassandraMailboxCountersDAO(Session session) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
    }

    public long countMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return cassandraAsyncExecutor.executeSingleRow(
            select(CassandraMailboxCountersTable.COUNT)
                .from(CassandraMailboxCountersTable.TABLE_NAME)
                .where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())))
            .thenApply(optional -> optional.map(row -> row.getLong(CassandraMailboxCountersTable.COUNT)))
            .join()
            .orElse(0L);
    }

    public long countUnseenMessagesInMailbox(Mailbox mailbox) throws MailboxException {
        CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
        return cassandraAsyncExecutor.executeSingleRow(
            select(CassandraMailboxCountersTable.UNSEEN)
                .from(CassandraMailboxCountersTable.TABLE_NAME)
                .where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())))
            .thenApply(optional -> optional.map(row -> row.getLong(CassandraMailboxCountersTable.UNSEEN)))
            .join()
            .orElse(0L);
    }

    public CompletableFuture<Void> decrementCount(MailboxId mailboxId) {
        CassandraId cassandraId = (CassandraId) mailboxId;
        return updateMailbox(cassandraId, decr(CassandraMailboxCountersTable.COUNT));
    }

    public CompletableFuture<Void> incrementCount(MailboxId mailboxId) {
        CassandraId cassandraId = (CassandraId) mailboxId;
        return updateMailbox(cassandraId, incr(CassandraMailboxCountersTable.COUNT));
    }

    public CompletableFuture<Void> decrementUnseen(MailboxId mailboxId) {
        CassandraId cassandraId = (CassandraId) mailboxId;
        return updateMailbox(cassandraId, decr(CassandraMailboxCountersTable.UNSEEN));
    }

    public CompletableFuture<Void> incrementUnseen(MailboxId mailboxId) {
        CassandraId cassandraId = (CassandraId) mailboxId;
        return updateMailbox(cassandraId, incr(CassandraMailboxCountersTable.UNSEEN));
    }

    public CompletableFuture<Void> updateMailbox(CassandraId mailboxId, Assignment operation) {
        return cassandraAsyncExecutor.executeVoid(
            update(CassandraMailboxCountersTable.TABLE_NAME)
                .with(operation)
                .where(eq(CassandraMailboxCountersTable.MAILBOX_ID, mailboxId.asUuid())));
    }

}
