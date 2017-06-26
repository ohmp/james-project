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

package org.apache.james.mpt.imapmailbox.suite;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mpt.api.ImapFeatures.Feature;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.suite.base.BasicImapCommands;
import org.apache.james.mpt.imapmailbox.suite.base.LocaleParametrizedTest;
import org.apache.james.mpt.script.SimpleScriptedTestProtocol;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public abstract class AuthenticatedState extends LocaleParametrizedTest {

    
    protected abstract ImapHostSystem createImapHostSystem();
    
    private ImapHostSystem system;
    private SimpleScriptedTestProtocol simpleScriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = createImapHostSystem();
        simpleScriptedTestProtocol = new SimpleScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
                .withUser(BasicImapCommands.USER, BasicImapCommands.PASSWORD)
                .withLocale(locale);
        BasicImapCommands.welcome(simpleScriptedTestProtocol);
        BasicImapCommands.authenticate(simpleScriptedTestProtocol);
    }
    
    @Test
    public void testNoop() throws Exception {
        simpleScriptedTestProtocol.run("Noop");
    }

    @Test
    public void testLogout() throws Exception {
        simpleScriptedTestProtocol.run("Logout");
    }

    @Test
    public void testCapability() throws Exception {
        simpleScriptedTestProtocol.run("Capability");
    }

    @Test
    public void testAppendExamineInbox() throws Exception {
        simpleScriptedTestProtocol.run("AppendExamineInbox");
    }

    @Test
    public void testAppendSelectInbox() throws Exception {
        simpleScriptedTestProtocol.run("AppendSelectInbox");
    }

    @Test
    public void testCreate() throws Exception {
        simpleScriptedTestProtocol.run("Create");
    }

    @Test
    public void testExamineEmpty() throws Exception {
        simpleScriptedTestProtocol.run("ExamineEmpty");
    }

    @Test
    public void testSelectEmpty() throws Exception {
        simpleScriptedTestProtocol.run("SelectEmpty");
    }

    @Test
    public void testListNamespace() throws Exception {
        simpleScriptedTestProtocol.run("ListNamespace");
    }

    @Test
    public void testListMailboxes() throws Exception {
        simpleScriptedTestProtocol.run("ListMailboxes");
    }

    @Test
    public void testStatus() throws Exception {
        simpleScriptedTestProtocol.run("Status");
    }

    @Test
    public void testSubscribe() throws Exception {
        simpleScriptedTestProtocol.run("Subscribe");
    }

    @Test
    public void testDelete() throws Exception {
        simpleScriptedTestProtocol.run("Delete");
    }

    @Test
    public void testAppend() throws Exception {
        simpleScriptedTestProtocol.run("Append");
    }

    @Test
    public void testAppendExpunge() throws Exception {
        simpleScriptedTestProtocol.run("AppendExpunge");
    }

    @Test
    public void testSelectAppend() throws Exception {
        simpleScriptedTestProtocol.run("SelectAppend");
    }
    
    @Test
    public void testStringArgs() throws Exception {
        simpleScriptedTestProtocol.run("StringArgs");
    }

    @Test
    public void testValidNonAuthenticated() throws Exception {
        simpleScriptedTestProtocol.run("ValidNonAuthenticated");
    }

    @Test
    public void listShouldNotListMailboxWithOtherNamspace() throws Exception {
        Assume.assumeTrue(system.supports(Feature.NAMESPACE_SUPPORT));
        system.createMailbox(new MailboxPath("#namespace", BasicImapCommands.USER, "Other"));
        simpleScriptedTestProtocol.run("ListMailboxes");
    }

    @Test
    public void listShouldNotListMailboxWithOtherUser() throws Exception {
        system.createMailbox(new MailboxPath("#namespace", BasicImapCommands.USER + "2", "Other"));
        simpleScriptedTestProtocol.run("ListMailboxes");
    }

}
