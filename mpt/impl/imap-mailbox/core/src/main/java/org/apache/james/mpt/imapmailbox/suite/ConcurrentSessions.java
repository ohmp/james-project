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

public abstract class ConcurrentSessions extends LocaleParametrizedTest implements ImapTestConstants {

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
    }

    @Test
    public void testConcurrentExpungeResponseUS() throws Exception {
          simpleScriptedTestProtocol.run("ConcurrentExpungeResponse");
    }

    @Test
    public void testConcurrentCrossExpungeUS() throws Exception {
          simpleScriptedTestProtocol.run("ConcurrentCrossExpunge");
    }
    
    @Test
    public void testConcurrentRenameSelectedSubUS() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentRenameSelectedSub");
    }

    @Test
    public void testConcurrentExistsResponseUS() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentExistsResponse");
    }

    @Test
    public void testConcurrentDeleteSelectedUS() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentDeleteSelected");
    }

    @Test
    public void testConcurrentFetchResponseUS() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentFetchResponse");
    }

    @Test
    public void testConcurrentRenameSelectedUS() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentRenameSelected");
    }

    @Test
    public void expungeShouldNotBreakUIDToMSNMapping() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentExpungeUIDToMSNMapping");
    }

    @Test
    public void appendShouldNotBreakUIDToMSNMapping() throws Exception {
        simpleScriptedTestProtocol.run("ConcurrentAppendUIDToMSNMapping");
    }
}
