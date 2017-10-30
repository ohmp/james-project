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

package org.apache.james.mpt.imapmailbox.cassandra;

import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.cassandra.host.CassandraHostSystem;
import org.apache.james.mpt.imapmailbox.suite.AuthenticatePlain;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.github.fge.lambdas.Throwing;

public class CassandraAuthenticatePlain extends AuthenticatePlain {

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();

    private static CassandraHostSystem system;

    @BeforeClass
    public static void setUpClass() throws Exception {
        system = new CassandraHostSystem(cassandraServer.getIp(), cassandraServer.getBindingPort());
        system.beforeTest();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected ImapHostSystem createImapHostSystem() {
        return system;
    }

    @After
    public void tearDown() throws Exception {
        MailboxManager mailboxManager = system.getMailboxManager();
        MailboxSession systemSession = mailboxManager.createSystemSession("mpt");
        mailboxManager.list(systemSession)
            .forEach(Throwing.consumer(
                mailboxPath -> mailboxManager.deleteMailbox(
                    mailboxPath,
                    mailboxManager.createSystemSession(mailboxPath.getUser()))));
    }
    
}
