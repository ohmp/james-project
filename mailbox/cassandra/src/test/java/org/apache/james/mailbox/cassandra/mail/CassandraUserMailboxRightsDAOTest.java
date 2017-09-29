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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.Rfc4314Rights;
import org.apache.james.mailbox.model.MailboxACL.Right;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class CassandraUserMailboxRightsDAOTest {

    private static final String USER_NAME = "userName";
    private static final String OTHER_USER_NAME = "otherUserName";
    private static final CassandraId MAILBOX_ID = CassandraId.timeBased();
    private static final CassandraId OTHER_MAILBOX_ID = CassandraId.timeBased();
    private static final Rfc4314Rights RIGHTS = MailboxACL.FULL_RIGHTS;
    private static final Rfc4314Rights OTHER_RIGHTS = new Rfc4314Rights(Right.Administer, Right.Read);

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();
    
    private CassandraCluster cassandra;

    private CassandraUserMailboxRightsDAO testee;

    @Before
    public void setUp() throws Exception {
        cassandra = CassandraCluster.create(new CassandraAclModule(), cassandraServer.getIp(), cassandraServer.getBindingPort());
        testee = new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
    }

    @After
    public void tearDown() throws Exception {
        cassandra.close();
    }

    @Test
    public void saveShouldInsertNewEntry() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();

        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join())
            .contains(RIGHTS);
    }

    @Test
    public void saveOnSecondShouldOverwrite() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();
        testee.save(USER_NAME, MAILBOX_ID, OTHER_RIGHTS).join();

        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join())
            .contains(OTHER_RIGHTS);
    }

    @Test
    public void retrieveShouldReturnEmptyWhenEmptyData() throws Exception {
        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join()
            .isPresent())
            .isFalse();
    }

    @Test
    public void retrieveShouldReturnStoredData() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();

        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join())
            .contains(RIGHTS);
    }

    @Test
    public void retrieveUserShouldReturnEmptyWhenEmptyData() throws Exception {
        assertThat(testee.retrieveUser(USER_NAME).join())
            .isEmpty();
    }

    @Test
    public void retrieveUserShouldReturnOnlyUserMailboxes() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();
        testee.save(USER_NAME, OTHER_MAILBOX_ID, OTHER_RIGHTS).join();
        testee.save(OTHER_USER_NAME, MAILBOX_ID, RIGHTS).join();

        assertThat(testee.retrieveUser(USER_NAME).join())
            .containsOnly(MapEntry.entry(MAILBOX_ID, RIGHTS), MapEntry.entry(OTHER_MAILBOX_ID, OTHER_RIGHTS));
    }

    @Test
    public void deleteShouldNotThrowWhenNone() throws Exception {
        testee.delete(USER_NAME, MAILBOX_ID).join();
    }

    @Test
    public void deleteShouldDeleteWhenExisting() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();

        testee.delete(USER_NAME, MAILBOX_ID).join();

        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join())
            .isEmpty();
    }

    @Test
    public void deleteUserShouldNotThrowWhenNone() throws Exception {
        testee.deleteUser(USER_NAME).join();
    }

    @Test
    public void deleteUserShouldDeleteWhenAllEntries() throws Exception {
        testee.save(USER_NAME, MAILBOX_ID, RIGHTS).join();
        testee.save(USER_NAME, OTHER_MAILBOX_ID, RIGHTS).join();

        testee.deleteUser(USER_NAME).join();

        assertThat(testee.retrieve(USER_NAME, MAILBOX_ID).join())
            .isEmpty();
    }
}
