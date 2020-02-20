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

package org.apache.james.mailbox.cassandra.mail.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.TestingSession.Barrier;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionModule;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.core.Username;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraIdAndPath;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImpl;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathV2DAO;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAO;
import org.apache.james.mailbox.cassandra.mail.task.SolveMailboxInconsistenciesService.Context;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxAssertingTool;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.task.Task.Result;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

class SolveMailboxInconsistenciesServiceTest {
    private static final int UID_VALIDITY_1 = 145;
    private static final int UID_VALIDITY_2 = 147;
    private static final Username USER = Username.of("user");
    private static final MailboxPath MAILBOX_PATH = MailboxPath.forUser(USER, "abc");
    private static final MailboxPath NEW_MAILBOX_PATH = MailboxPath.forUser(USER, "xyz");
    private static final int GRACE_PERIOD_MILLIS = 500;
    private static final Duration GRACE_PERIOD = Duration.ofMillis(GRACE_PERIOD_MILLIS);
    private static CassandraId CASSANDRA_ID_1 = CassandraId.timeBased();
    private static final Mailbox MAILBOX = new Mailbox(MAILBOX_PATH, UID_VALIDITY_1, CASSANDRA_ID_1);
    private static CassandraId CASSANDRA_ID_2 = CassandraId.timeBased();

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(
            CassandraSchemaVersionModule.MODULE,
            CassandraMailboxModule.MODULE,
            CassandraAclModule.MODULE));


    CassandraMailboxDAO mailboxDAO;
    CassandraMailboxPathV2DAO mailboxPathV2DAO;
    CassandraSchemaVersionDAO versionDAO;
    CassandraMailboxMapper mapper;
    SolveMailboxInconsistenciesService testee;

    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        mailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
        mailboxPathV2DAO = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        versionDAO = new CassandraSchemaVersionDAO(cassandra.getConf());
        testee = new SolveMailboxInconsistenciesService(mailboxDAO, mailboxPathV2DAO, versionDAO, GRACE_PERIOD);

        mapper = createMailboxMapper(cassandra);

        versionDAO.updateVersion(new SchemaVersion(7)).block();
    }

    private CassandraMailboxMapper createMailboxMapper(CassandraCluster cassandra) {
        CassandraUserMailboxRightsDAO userMailboxRightsDAO = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        return new CassandraMailboxMapper(mailboxDAO,
            new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider()),
            mailboxPathV2DAO,
            userMailboxRightsDAO,
            new CassandraACLMapper(cassandra.getConf(), userMailboxRightsDAO, CassandraConfiguration.builder().build()));
    }

    @Test
    void fixMailboxInconsistenciesShouldFailWhenIsBelowMailboxPathV2Migration() {
        versionDAO.truncateVersion().block();
        versionDAO.updateVersion(new SchemaVersion(5)).block();

        assertThatThrownBy(() -> testee.fixMailboxInconsistencies(new Context()).block())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Schema version 6 is required in order to ensure mailboxPathV2DAO to be correctly populated, got Optional[5]");
    }

    @Test
    void fixMailboxInconsistenciesShouldFailWhenVersionIsMissing() {
        versionDAO.truncateVersion().block();

        assertThatThrownBy(() -> testee.fixMailboxInconsistencies(new Context()).block())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Schema version 6 is required in order to ensure mailboxPathV2DAO to be correctly populated, got Optional.empty");
    }

    @Test
    void fixMailboxInconsistenciesShouldNotFailWhenIsEqualToMailboxPathV2Migration() {
        versionDAO.truncateVersion().block();
        versionDAO.updateVersion(new SchemaVersion(6)).block();

        assertThatCode(() -> testee.fixMailboxInconsistencies(new Context()).block())
            .doesNotThrowAnyException();
    }

    @Test
    void fixMailboxInconsistenciesShouldNotFailWhenIsAboveMailboxPathV2Migration() {
        versionDAO.truncateVersion().block();
        versionDAO.updateVersion(new SchemaVersion(7)).block();

        assertThatCode(() -> testee.fixMailboxInconsistencies(new Context()).block())
            .doesNotThrowAnyException();
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnCompletedWhenNoData() {
        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.COMPLETED);
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnCompletedWhenConsistentData() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.COMPLETED);
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnCompletedWhenOrphanMailboxData() {
        mailboxDAO.save(MAILBOX).block();

        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.COMPLETED);
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnCompletedWhenOrphanPathData() {
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.COMPLETED);
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnPartialWhenDAOMisMatchOnId() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();

        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.PARTIAL);
    }

    @Test
    void fixMailboxInconsistenciesShouldReturnPartialWhenDAOMisMatchOnPath() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(NEW_MAILBOX_PATH, CASSANDRA_ID_1).block();

        assertThat(testee.fixMailboxInconsistencies(new Context()).block())
            .isEqualTo(Result.PARTIAL);
    }

    @Test
    void fixMailboxInconsistenciesShouldNotUpdateContextWhenNoData() {
        Context context = new Context();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context).isEqualToComparingFieldByFieldRecursively(new Context());
    }

    @Test
    void fixMailboxInconsistenciesShouldUpdateContextWhenConsistentData() {
        Context context = new Context();
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context)
            .isEqualTo(Context.builder()
                .processedMailboxEntries(1)
                .processedMailboxPathEntries(1)
                .build());
    }

    @Test
    void fixMailboxInconsistenciesShouldUpdateContextWhenOrphanMailboxData() {
        Context context = new Context();
        mailboxDAO.save(MAILBOX).block();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context)
            .isEqualTo(Context.builder()
                .processedMailboxEntries(1)
                .fixedInconsistencies(1)
                .build());
    }

    @Test
    void fixMailboxInconsistenciesShouldUpdateContextWhenOrphanPathData() {
        Context context = new Context();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context)
            .isEqualTo(Context.builder()
                .processedMailboxPathEntries(1)
                .fixedInconsistencies(1)
                .build());
    }

    @Test
    void fixMailboxInconsistenciesShouldUpdateContextWhenDAOMisMatchOnId() {
        Context context = new Context();
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();
        mailboxDAO.save(new Mailbox(MAILBOX_PATH, UID_VALIDITY_2, CASSANDRA_ID_2)).block();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context)
            .isEqualTo(Context.builder()
                .processedMailboxEntries(2)
                .processedMailboxPathEntries(1)
                .fixedInconsistencies(0)
                .addConflictingEntry(ConflictingEntry.builder()
                    .mailboxDaoEntry(MAILBOX)
                    .mailboxPathDaoEntry(MAILBOX_PATH, CASSANDRA_ID_2))
                .build());
    }

    @Test
    void fixMailboxInconsistenciesShouldUpdateContextWhenDAOMisMatchOnPath() {
        Context context = new Context();
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(NEW_MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(context).block();

        assertThat(context)
            .isEqualTo(Context.builder()
                .processedMailboxEntries(1)
                .processedMailboxPathEntries(1)
                .fixedInconsistencies(1)
                .addConflictingEntry(ConflictingEntry.builder()
                    .mailboxDaoEntry(MAILBOX)
                    .mailboxPathDaoEntry(NEW_MAILBOX_PATH, CASSANDRA_ID_1))
                .build());
    }

    @Test
    void fixMailboxInconsistenciesShouldNotAlterStateWhenEmpty() {
        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block()).isEmpty();
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block()).isEmpty();
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldNotAlterStateWhenConsistent() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(new CassandraIdAndPath(CASSANDRA_ID_1, MAILBOX_PATH));
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldAlterStateWhenOrphanMailbox() {
        mailboxDAO.save(MAILBOX).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(new CassandraIdAndPath(CASSANDRA_ID_1, MAILBOX_PATH));
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldAlterStateWhenOrphanMailboxPath() {
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .isEmpty();
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .isEmpty();
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldNotAlterStateWhenLoop() {
        mailboxDAO.save(MAILBOX).block();
        Mailbox mailbox2 = new Mailbox(NEW_MAILBOX_PATH, UID_VALIDITY_2, CASSANDRA_ID_2);
        mailboxDAO.save(mailbox2).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();
        mailboxPathV2DAO.save(NEW_MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX, mailbox2);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(
                    new CassandraIdAndPath(CASSANDRA_ID_1, NEW_MAILBOX_PATH),
                    new CassandraIdAndPath(CASSANDRA_ID_2, MAILBOX_PATH));
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldAlterStateWhenDaoMisMatchOnPath() {
        // Note that CASSANDRA_ID_1 becomes usable
        // However in order to avoid data loss, merging CASSANDRA_ID_1 and CASSANDRA_ID_2 is still required
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(NEW_MAILBOX_PATH, CASSANDRA_ID_1).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(
                    new CassandraIdAndPath(CASSANDRA_ID_1, NEW_MAILBOX_PATH),
                    new CassandraIdAndPath(CASSANDRA_ID_1, MAILBOX_PATH));
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldAlterStateWhenDaoMisMatchOnId() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .isEmpty();
        });
    }

    @Test
    void multipleRunShouldDaoMisMatchOnId() {
        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();

        testee.fixMailboxInconsistencies(new Context()).block();
        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(new CassandraIdAndPath(CASSANDRA_ID_1, MAILBOX_PATH));
        });
    }

    @Test
    void fixMailboxInconsistenciesShouldNotAlterStateWhenTwoEntriesWithSamePath() {
        // Both mailbox merge is required
        Mailbox mailbox2 = new Mailbox(MAILBOX_PATH, UID_VALIDITY_2, CASSANDRA_ID_2);

        mailboxDAO.save(MAILBOX).block();
        mailboxPathV2DAO.save(MAILBOX_PATH, CASSANDRA_ID_2).block();
        mailboxDAO.save(mailbox2).block();

        testee.fixMailboxInconsistencies(new Context()).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mailboxDAO.retrieveAllMailboxes().collectList().block())
                .containsExactlyInAnyOrder(MAILBOX, mailbox2);
            softly.assertThat(mailboxPathV2DAO.listAll().collectList().block())
                .containsExactlyInAnyOrder(
                    new CassandraIdAndPath(CASSANDRA_ID_2, MAILBOX_PATH));
        });
    }

    @Test
    void concurrentCreateShouldNotBeConsideredAsAnInconsistency(CassandraCluster cassandra) throws Exception {
        Barrier barrier = new Barrier();

        cassandra.getConf()
            .awaitOn(barrier)
            .whenBoundStatementStartsWith("INSERT INTO mailbox ")
            .times(6)
            .setExecutionHook();

        // Start create a mailbox. Path registration entry will be created.
        // However we instrument the driver to block mailbox entry until barrier is released
        Mono<Mailbox> createMailbox = Mono.fromCallable(() -> mapper.create(MAILBOX_PATH, UID_VALIDITY_1))
            .cache();
        createMailbox.subscribeOn(Schedulers.elastic())
            .subscribe();
        barrier.awaitCaller();

        // Start fixing inconsistencies
        Context context = new Context();
        ConcurrentTestRunner runner = ConcurrentTestRunner.builder()
            .operation((a, b) -> testee.fixMailboxInconsistencies(context).block())
            .threadCount(1)
            .operationCount(1)
            .run();

        // Let the 'fix inconsistencies' make some progress...
        Thread.sleep(GRACE_PERIOD_MILLIS / 2);
        barrier.releaseCaller();

        runner.awaitTermination(Duration.ofMinutes(1));
        SoftAssertions.assertSoftly(softly -> {
            // Fail on concurrent modification
            softly.assertThat(context)
                .isEqualTo(Context.builder()
                    .processedMailboxPathEntries(1)
                    .errors(1)
                    .build());

            // Don't alter DB state
            Mailbox mailbox = createMailbox.block();
            CassandraId mailboxId = (CassandraId) mailbox.getMailboxId();
            MailboxAssertingTool.softly(softly)
                .assertThat(mailboxDAO.retrieveMailbox(mailboxId).block())
                .isEqualTo(mailbox);
            assertThat(mailboxPathV2DAO.retrieveId(MAILBOX_PATH).block())
                .isEqualTo(new CassandraIdAndPath(mailboxId, MAILBOX_PATH));
        });
    }
}