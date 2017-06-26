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
import org.junit.Before;
import org.junit.Test;

public abstract class SelectedState extends LocaleParametrizedTest implements ImapTestConstants {

    protected abstract ImapHostSystem createImapHostSystem();
    
    private ImapHostSystem system;
    private SimpleScriptedTestProtocol simpleScriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = createImapHostSystem();
        simpleScriptedTestProtocol = new SimpleScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
                .withUser(USER, PASSWORD)
                .withLocale(locale);
        BasicImapCommands.welcome(simpleScriptedTestProtocol);
        BasicImapCommands.authenticate(simpleScriptedTestProtocol);
        BasicImapCommands.prepareMailbox(simpleScriptedTestProtocol);
    }
    
    @Test
    public void testCheck() throws Exception {
        simpleScriptedTestProtocol.run("Check");
    }

    @Test
    public void testExpunge() throws Exception {
        simpleScriptedTestProtocol.run("Expunge");
    }

    @Test
    public void testSearch() throws Exception {
        simpleScriptedTestProtocol.run("Search");
    }

    @Test
    public void testFetchSingleMessage() throws Exception {
        simpleScriptedTestProtocol.run("FetchSingleMessage");
    }

    @Test
    public void testFetchMultipleMessages() throws Exception {
        simpleScriptedTestProtocol.run("FetchMultipleMessages");
    }

    @Test
    public void testFetchPeek() throws Exception {
        simpleScriptedTestProtocol.run("FetchPeek");
    }

    @Test
    public void testStore() throws Exception {
        simpleScriptedTestProtocol.run("Store");
    }

    @Test
    public void testCopy() throws Exception {
        simpleScriptedTestProtocol.run("Copy");
    }

    @Test
    public void testUid() throws Exception {
        simpleScriptedTestProtocol.run("Uid");
    }
    
    @Test
    public void testNamespace() throws Exception {
        simpleScriptedTestProtocol.run("Namespace");
    }

}
