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

import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.ImapTestConstants;
import org.apache.james.mpt.imapmailbox.suite.base.BasicImapCommands;
import org.apache.james.mpt.imapmailbox.suite.base.LocaleParametrizedTest;
import org.apache.james.mpt.script.SimpleScriptedTestProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public abstract class SelectedInbox extends LocaleParametrizedTest implements ImapTestConstants {

    protected abstract Module createModule();
    
    private ImapHostSystem system;
    private SimpleScriptedTestProtocol simpleScriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = Guice.createInjector(createModule()).getInstance(ImapHostSystem.class);
        simpleScriptedTestProtocol = new SimpleScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
                .withUser(USER, PASSWORD)
                .withLocale(locale);
        BasicImapCommands.welcome(simpleScriptedTestProtocol);
        BasicImapCommands.authenticate(simpleScriptedTestProtocol);
        BasicImapCommands.selectInbox(simpleScriptedTestProtocol);
    }

    @After
    public void tearDown() throws Exception {
        system.afterTest();
    }
    
    @Test
    public void testValidNonAuthenticatedUS() throws Exception {
        simpleScriptedTestProtocol.run("ValidNonAuthenticated");
    }

    @Test
    public void testCapabilityUS() throws Exception {
        simpleScriptedTestProtocol.run("Capability");
    }

    @Test
    public void testNoopUS() throws Exception {
        simpleScriptedTestProtocol.run("Noop");
    }

    @Test
    public void testLogoutUS() throws Exception {
        simpleScriptedTestProtocol.run("Logout");
    }

    @Test
    public void testCreateUS() throws Exception {
        simpleScriptedTestProtocol.run("Create");
    }

    @Test
    public void testExamineEmptyUS() throws Exception {
        simpleScriptedTestProtocol.run("ExamineEmpty");
    }

    @Test
    public void testSelectEmptyUS() throws Exception {
        simpleScriptedTestProtocol.run("SelectEmpty");
    }

    @Test
    public void testListNamespaceUS() throws Exception {
        simpleScriptedTestProtocol.run("ListNamespace");
    }

    @Test
    public void testListMailboxesUS() throws Exception {
        simpleScriptedTestProtocol.run("ListMailboxes");
    }

    @Test
    public void testStatusUS() throws Exception {
        simpleScriptedTestProtocol.run("Status");
    }

    @Test
    public void testStringArgsUS() throws Exception {
        simpleScriptedTestProtocol.run("StringArgs");
    }

    @Test
    public void testSubscribeUS() throws Exception {
        simpleScriptedTestProtocol.run("Subscribe");
    }

    @Test
    public void testAppendUS() throws Exception {
        simpleScriptedTestProtocol.run("Append");
    }

    @Test
    public void testDeleteUS() throws Exception {
        simpleScriptedTestProtocol.run("Delete");
    }

}
