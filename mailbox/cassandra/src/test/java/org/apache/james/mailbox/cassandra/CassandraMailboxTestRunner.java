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

package org.apache.james.mailbox.cassandra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.cassandra.CassandraBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.mailbox.cassandra.event.distributed.CassandraMailboxPathRegisterMapper;
import org.apache.james.mailbox.cassandra.event.distributed.CassandraMailboxPathRegistrerMapperTest;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapperTest;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2Test;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentOwnerDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentOwnerDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapperConcurrencyTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapperTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImpl;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImplTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathV2DAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAOTest;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProviderTest;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProviderTest;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAOTest;
import org.apache.james.mailbox.cassandra.mail.utils.GuiceUtils;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.modules.CassandraApplicableFlagsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraAttachmentModule;
import org.apache.james.mailbox.cassandra.modules.CassandraDeletedMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraFirstUnseenModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxCounterModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMailboxRecentsModule;
import org.apache.james.mailbox.cassandra.modules.CassandraMessageModule;
import org.apache.james.mailbox.cassandra.modules.CassandraModSeqModule;
import org.apache.james.mailbox.cassandra.modules.CassandraRegistrationModule;
import org.apache.james.mailbox.cassandra.modules.CassandraUidModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

class CassandraMailboxTestRunner extends CassandraTestRunner {
    @Nested
    class ApplicableFlagDAOTest extends Runner implements CassandraApplicableFlagDAOTest {
        private CassandraApplicableFlagDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraApplicableFlagsModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraApplicableFlagDAO(cassandra.getConf());
        }

        @Override
        public CassandraApplicableFlagDAO testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentDAOTest extends Runner implements CassandraAttachmentDAOTest {
        private CassandraAttachmentDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentDAO(cassandra.getConf(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CassandraConfiguration.DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraAttachmentDAO testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentDAOV2Test extends Runner implements CassandraAttachmentDAOV2Test {
        private CassandraAttachmentDAOV2 testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_V2_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentDAOV2(BLOB_ID_FACTORY, cassandra.getConf());
        }

        @Override
        public CassandraAttachmentDAOV2 testee() {
            return testee;
        }
    }

    @Nested
    class AttachmentOwnerDAOTest extends Runner implements CassandraAttachmentOwnerDAOTest {
        private CassandraAttachmentOwnerDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAttachmentModule.CASSANDRA_ATTACHMENT_OWNER_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraAttachmentOwnerDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraAttachmentOwnerDAO testee() {
            return testee;
        }
    }

    @Nested
    class DeletedMessageDAOTest extends Runner implements CassandraDeletedMessageDAOTest {
        private CassandraDeletedMessageDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraDeletedMessageModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraDeletedMessageDAO(cassandra.getConf());
        }

        @Override
        public CassandraDeletedMessageDAO testee() {
            return testee;
        }
    }

    @Nested
    class FirstUnseenDAOTest extends Runner implements CassandraFirstUnseenDAOTest {
        private CassandraFirstUnseenDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraFirstUnseenModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraFirstUnseenDAO(cassandra.getConf());
        }

        @Override
        public CassandraFirstUnseenDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxCounterDAOTest extends Runner implements CassandraMailboxCounterDAOTest {
        private CassandraMailboxCounterDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraMailboxCounterModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxCounterDAO(cassandra.getConf());
        }

        @Override
        public CassandraMailboxCounterDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxDAOTest extends Runner implements CassandraMailboxDAOTest {
        private CassandraMailboxDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_BASE_TYPE,
                CassandraMailboxModule.MAILBOX_TABLE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
        }

        @Override
        public CassandraMailboxDAO testee() {
            return testee;
        }

    }

    @Nested
    class UserMailboxRightsDAOTest extends Runner implements CassandraUserMailboxRightsDAOTest {
        private CassandraUserMailboxRightsDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraAclModule.CASSANDRA_USER_MAILBOX_RIGHTS_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraUserMailboxRightsDAO testee() {
            return testee;
        }
    }

    @Nested
    class UidProviderTest extends Runner implements CassandraUidProviderTest {
        private CassandraUidProvider uidProvider;

        @Override
        public CassandraModule module() {
            return new CassandraUidModule();
        }

        @BeforeEach
        void setUp() {
            uidProvider = new CassandraUidProvider(cassandra.getConf());
        }

        @Override
        public CassandraUidProvider testee() {
            return uidProvider;
        }
    }

    @Nested
    class ModSedProviderTest extends Runner implements CassandraModSeqProviderTest {
        private CassandraModSeqProvider modSeqProvider;

        @Override
        public CassandraModule module() {
            return new CassandraModSeqModule();
        }

        @BeforeEach
        void setUp() {
            modSeqProvider = new CassandraModSeqProvider(cassandra.getConf());
        }

        @Override
        public CassandraModSeqProvider testee() {
            return modSeqProvider;
        }
    }

    @Nested
    class MessageIdToImapUidDAOTest extends Runner implements CassandraMessageIdToImapUidDAOTest {
        private CassandraMessageIdToImapUidDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMessageModule.MESSAGE_ID_TO_IMAP_UID_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMessageIdToImapUidDAO(cassandra.getConf(), messageIdFactory);
        }

        @Override
        public CassandraMessageIdToImapUidDAO testee() {
            return testee;
        }
    }

    @Nested
    class MessageIdDAOTest extends Runner implements CassandraMessageIdDAOTest {
        private CassandraMessageIdDAO testee;

        @Override
        public CassandraModule module() {
            return CassandraMessageModule.CASSANDRA_MESSAGE_ID_TABLE;
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMessageIdDAO(cassandra.getConf(), messageIdFactory);
        }

        @Override
        public CassandraMessageIdDAO testee() {
            return testee;
        }
    }

    @Nested
    class MessageDAOTest extends Runner implements CassandraMessageDAOTest {
        private CassandraMessageDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(CassandraMessageModule.ATTACHMENT_TYPE,
                CassandraMessageModule.PROPERTY_TYPE,
                CassandraMessageModule.CASSANDRA_MESSAGE_V2_TABLE,
                new CassandraBlobModule());
        }

        @BeforeEach
        void setUp() {
            CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());
            CassandraBlobId.Factory blobIdFactory = new CassandraBlobId.Factory();
            testee = new CassandraMessageDAO(cassandra.getConf(), cassandra.getTypesProvider(), blobsDAO, blobIdFactory,
                CassandraUtils.WITH_DEFAULT_CONFIGURATION, new CassandraMessageId.Factory());
        }

        @Override
        public CassandraMessageDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxRecentDAOTest extends Runner implements CassandraMailboxRecentDAOTest {
        private CassandraMailboxRecentsDAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraMailboxRecentsModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxRecentsDAO(cassandra.getConf());
        }

        @Override
        public CassandraMailboxRecentsDAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathDAOImplTest extends Runner implements CassandraMailboxPathDAOImplTest {
        protected CassandraMailboxPathDAOImpl testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_PATH_TABLE,
                CassandraMailboxModule.MAILBOX_BASE_TYPE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider());
        }

        @Override
        public CassandraMailboxPathDAOImpl testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathDAOV2Test extends Runner implements CassandraMailboxPathDAOTest {
        protected CassandraMailboxPathV2DAO testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(
                CassandraMailboxModule.MAILBOX_PATH_V2_TABLE,
                CassandraMailboxModule.MAILBOX_BASE_TYPE);
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        }

        @Override
        public CassandraMailboxPathV2DAO testee() {
            return testee;
        }
    }

    @Nested
    class MailboxMapperConcurrencyTest extends Runner implements CassandraMailboxMapperConcurrencyTest {
        private CassandraMailboxMapper testee;

        @Override
        public CassandraModule module() {
            return  new CassandraModuleComposite(new CassandraMailboxModule(), new CassandraAclModule());
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra)
                .getInstance(CassandraMailboxMapper.class);
        }

        @Override
        public CassandraMailboxMapper testee() {
            return testee;
        }
    }

    @Nested
    class MailboxMapperTest extends Runner implements CassandraMailboxMapperTest {

        private Testee testee;

        @Override
        public CassandraModule module() {
            return new CassandraModuleComposite(new CassandraMailboxModule(), new CassandraAclModule());
        }

        @BeforeEach
        void setUp() {
            CassandraUserMailboxRightsDAO userMailboxRightsDAO = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            CassandraMailboxDAO mailboxDAO = new CassandraMailboxDAO(cassandra.getConf(), cassandra.getTypesProvider());
            CassandraMailboxPathDAOImpl mailboxPathDAO = new CassandraMailboxPathDAOImpl(cassandra.getConf(), cassandra.getTypesProvider());
            CassandraMailboxPathV2DAO mailboxPathV2DAO = new CassandraMailboxPathV2DAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);

            CassandraMailboxMapper mailboxMapper = new CassandraMailboxMapper(
                mailboxDAO,
                mailboxPathDAO,
                mailboxPathV2DAO,
                userMailboxRightsDAO,
                new CassandraACLMapper(cassandra.getConf(),
                    new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
                    CassandraConfiguration.DEFAULT_CONFIGURATION));

            testee = new Testee(
                mailboxDAO,
                mailboxPathDAO,
                mailboxPathV2DAO,
                mailboxMapper);
        }

        @Override
        public Testee testee() {
            return testee;
        }

    }

    @Nested
    class ACLMapperTest extends Runner implements CassandraACLMapperTest {

        private CassandraACLMapper testee;
        private ExecutorService executor;

        @Override
        public CassandraModule module() {
            return new CassandraAclModule();
        }

        @BeforeEach
        void setUp() {
            testee = GuiceUtils.testInjector(cassandra())
                .getInstance(CassandraACLMapper.class);
            executor = Executors.newFixedThreadPool(2);
        }

        @AfterEach
        void tearDown() {
            executor.shutdownNow();
        }

        @Override
        public ExecutorService executor() {
            return executor;
        }

        @Override
        public CassandraCluster cassandra() {
            return cassandra;
        }

        @Override
        public CassandraACLMapper testee() {
            return testee;
        }
    }

    @Nested
    class MailboxPathRegisterMapperTest extends Runner implements CassandraMailboxPathRegistrerMapperTest {
        private CassandraMailboxPathRegisterMapper testee;

        @Override
        public CassandraModule module() {
            return new CassandraRegistrationModule();
        }

        @BeforeEach
        void setUp() {
            testee = new CassandraMailboxPathRegisterMapper(cassandra().getConf(),
                cassandra().getTypesProvider(),
                CassandraUtils.WITH_DEFAULT_CONFIGURATION,
                CASSANDRA_TIME_OUT_IN_S);
        }

        @Override
        public CassandraCluster cassandra() {
            return cassandra;
        }

        @Override
        public CassandraMailboxPathRegisterMapper testee() {
            return testee;
        }
    }
}
