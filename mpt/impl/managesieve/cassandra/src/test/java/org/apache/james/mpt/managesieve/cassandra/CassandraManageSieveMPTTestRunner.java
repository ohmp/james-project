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

package org.apache.james.mpt.managesieve.cassandra;

import org.apache.james.backends.cassandra.CassandraTestRunner;
import org.apache.james.mpt.host.ManageSieveHostSystem;
import org.apache.james.mpt.testsuite.AuthenticateTest;
import org.apache.james.mpt.testsuite.CapabilityTest;
import org.apache.james.mpt.testsuite.CheckScriptTest;
import org.apache.james.mpt.testsuite.DeleteScriptTest;
import org.apache.james.mpt.testsuite.GetScriptTest;
import org.apache.james.mpt.testsuite.HaveSpaceTest;
import org.apache.james.mpt.testsuite.ListScriptsTest;
import org.apache.james.mpt.testsuite.LogoutTest;
import org.apache.james.mpt.testsuite.NoopTest;
import org.apache.james.mpt.testsuite.PutScriptTest;
import org.apache.james.mpt.testsuite.RenameScriptTest;
import org.apache.james.mpt.testsuite.SetActiveTest;
import org.apache.james.mpt.testsuite.StartTlsTest;
import org.apache.james.mpt.testsuite.UnauthenticatedTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import com.google.inject.Guice;
import com.google.inject.Injector;

class CassandraManageSieveMPTTestRunner extends CassandraTestRunner {
    @Nested
    class CassandraAuthenticateTest implements AuthenticateTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraCapabilityTest implements CapabilityTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraCheckScriptTest implements CheckScriptTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraDeleteScriptTest implements DeleteScriptTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraGetScriptTest implements GetScriptTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraHaveSpaceTest implements HaveSpaceTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraListScriptsTest implements ListScriptsTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraLogoutTest implements LogoutTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraNoopTest implements NoopTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraPutScriptTest implements PutScriptTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraRenameScriptTest implements RenameScriptTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraSetActiveTest implements SetActiveTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraStartTlsTest implements StartTlsTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }

    @Nested
    class CassandraUnauthenticatedTest implements UnauthenticatedTest {
        private ManageSieveHostSystem system;

        @BeforeEach
        void setUp() throws Exception {
            Injector injector = Guice.createInjector(new CassandraModule(dockerCassandra.getHost()));
            system = injector.getInstance(ManageSieveHostSystem.class);
            system.beforeTest();
        }

        @Override
        public ManageSieveHostSystem hostSystem() {
            return system;
        }
    }
}
