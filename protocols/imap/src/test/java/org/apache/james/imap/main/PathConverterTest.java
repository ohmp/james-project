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

package org.apache.james.imap.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PathConverterTest {

    private static final String USERNAME = "username";
    private static final char PATH_DELIMITER = '.';
    public static final String BOB = "bob";

    private ImapSession imapSession;
    private MailboxSession mailboxSession;
    private PathConverter pathConverter;
    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        imapSession = mock(ImapSession.class);
        mailboxSession = mock(MailboxSession.class);
        MailboxSession.User user = mock(MailboxSession.User.class);
        pathConverter = PathConverter.forSession(imapSession);
        when(imapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mailboxSession);
        when(mailboxSession.getUser()).thenReturn(user);
        when(mailboxSession.getPathDelimiter()).thenReturn(PATH_DELIMITER);
        when(user.getUserName()).thenReturn(USERNAME);
    }

    @Test
    public void buildFullPathShouldThrowOnNull() {
        assertThatThrownBy(() -> pathConverter.buildFullPath(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildMailboxNameShouldThrowOnNull() {
        assertThatThrownBy(() -> pathConverter.buildMailboxName(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildFullPathShouldAcceptEmpty() {
        assertThat(pathConverter.buildFullPath(""))
            .isEqualTo(MailboxPath.forUser(USERNAME, ""));
    }

    @Test
    public void buildFullPathShouldAcceptRelativeMailboxName() {
        String mailboxName = "mailboxName";
        assertThat(pathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(USERNAME, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedMailbox() {
        String mailboxName = "mailboxName";
        assertThat(pathConverter.buildFullPath(PathConverter.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptSubFolder() {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(pathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(USERNAME, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedSubFolder() {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        assertThat(pathConverter.buildFullPath(PathConverter.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName + PATH_DELIMITER + subFolder));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUserAndPathSeparator() {
        assertThat(pathConverter.buildFullPath(
            PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(BOB, ""));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUser() {
        assertThat(pathConverter.buildFullPath(
            PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB))
            .isEqualTo(MailboxPath.forUser(USERNAME, PathConverter.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndPathDelimiter() {
        assertThat(pathConverter.buildFullPath(
            PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(USERNAME, PathConverter.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxOnly() {
        assertThat(pathConverter.buildFullPath(
            PathConverter.DELEGATED_MAILBOXES_BASE))
            .isEqualTo(MailboxPath.forUser(USERNAME, PathConverter.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildMailboxNameShouldAcceptRelativeMailboxName() {
        String mailboxName = "mailboxName";
        assertThat(pathConverter.buildMailboxName(MailboxPath.forUser(USERNAME, mailboxName)))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedMailbox() {
        String mailboxName = "mailboxName";
        String fullMailboxName = PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName;
        assertThat(pathConverter.buildMailboxName(MailboxPath.forUser(BOB, mailboxName)))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedSubFolder() {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(pathConverter.buildMailboxName(MailboxPath.forUser(USERNAME, mailboxName)))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptAbsoluteUserPathWithSubFolder() {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        String fullMailboxName = PathConverter.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder;
        MailboxPath mailboxPath = MailboxPath.forUser(BOB, mailboxName + PATH_DELIMITER + subFolder);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUserAndSeparator() {
        String mailboxName = PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUser() {
        String mailboxName = PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndSeparator() {
        String mailboxName = PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailbox() {
        String mailboxName = PathConverter.DELEGATED_MAILBOXES_BASE;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptEmptyDelegatedMailboxName() {
        String mailboxName = PathConverter.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(BOB, "");
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }
}
