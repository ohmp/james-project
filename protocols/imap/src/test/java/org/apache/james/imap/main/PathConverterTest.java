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

import java.util.List;

import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.protocols.imap.DefaultNamespaceConfiguration;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PathConverterTest {

    private static final String USERNAME = "username";
    private static final char PATH_DELIMITER = '.';
    public static final String BOB = "bob";
    public static final String PRIVATE_PREFIX = "#private";
    public static final String DELEGATED_PREFIX = "#delegated";
    public static final ImapSession.NamespaceConfiguration CUSTOM_NAMESPACE_CONFIGURATION = new ImapSession.NamespaceConfiguration() {
        @Override
        public String personalNamespace() {
            return PRIVATE_PREFIX;
        }

        @Override
        public String otherUsersNamespace() {
            return DELEGATED_PREFIX;
        }

        @Override
        public List<String> sharedNamespacesNamespaces() {
            return ImmutableList.of();
        }
    };
    public static final String MAILBOX_NAME = "toto";

    private ImapSession imapSession;
    private PathConverter pathConverter;

    @Before
    public void setUp() {
        imapSession = mock(ImapSession.class);
        MailboxSession mailboxSession = mock(MailboxSession.class);
        MailboxSession.User user = mock(MailboxSession.User.class);
        when(imapSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY)).thenReturn(mailboxSession);
        when(imapSession.getNamespaceConfiguration()).thenReturn(new DefaultNamespaceConfiguration());
        when(mailboxSession.getUser()).thenReturn(user);
        when(mailboxSession.getPathDelimiter()).thenReturn(PATH_DELIMITER);
        when(user.getUserName()).thenReturn(USERNAME);
        pathConverter = PathConverter.forSession(imapSession);
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
    public void buildFullPathShouldAcceptEmpty() throws MailboxNotFoundException {
        assertThat(pathConverter.buildFullPath(""))
            .isEqualTo(MailboxPath.forUser(USERNAME, ""));
    }

    @Test
    public void buildFullPathShouldAcceptRelativeMailboxName() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(pathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(USERNAME, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedMailbox() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(pathConverter.buildFullPath(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptSubFolder() throws MailboxNotFoundException {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(pathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(USERNAME, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedSubFolder() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        assertThat(pathConverter.buildFullPath(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName + PATH_DELIMITER + subFolder));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUserAndPathSeparator() throws MailboxNotFoundException {
        assertThat(pathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(BOB, ""));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUser() throws MailboxNotFoundException {
        assertThat(pathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB))
            .isEqualTo(MailboxPath.forUser(USERNAME, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndPathDelimiter() throws MailboxNotFoundException {
        assertThat(pathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(USERNAME, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxOnly() throws MailboxNotFoundException {
        assertThat(pathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE))
            .isEqualTo(MailboxPath.forUser(USERNAME, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
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
        String fullMailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName;
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
        String fullMailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder;
        MailboxPath mailboxPath = MailboxPath.forUser(BOB, mailboxName + PATH_DELIMITER + subFolder);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUserAndSeparator() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUser() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndSeparator() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailbox() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE;
        MailboxPath mailboxPath = MailboxPath.forUser(USERNAME, mailboxName);
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptEmptyDelegatedMailboxName() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB;
        MailboxPath mailboxPath = MailboxPath.forUser(BOB, "");
        assertThat(pathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildFullPathShouldHandlePrivateNamespace() throws MailboxNotFoundException {
        when(imapSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        pathConverter = PathConverter.forSession(imapSession);

        assertThat(
            pathConverter.buildFullPath(PRIVATE_PREFIX + PATH_DELIMITER + MAILBOX_NAME))
            .isEqualTo(
                MailboxPath.forUser(USERNAME, MAILBOX_NAME));
    }

    @Test
    public void buildFullPathShouldReturnNotFoundWhenOutOfAllNamespace() {
        when(imapSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        pathConverter = PathConverter.forSession(imapSession);

        assertThatThrownBy(() ->
            pathConverter.buildFullPath(MAILBOX_NAME))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    public void buildFullPathShouldReturnMailboxPathWhenUsingSpecificUserNamespace() throws MailboxNotFoundException {
        when(imapSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        pathConverter = PathConverter.forSession(imapSession);

        assertThat(
            pathConverter.buildFullPath(DELEGATED_PREFIX + PATH_DELIMITER +
                BOB + PATH_DELIMITER +
                MAILBOX_NAME))
            .isEqualTo(
                MailboxPath.forUser(BOB, MAILBOX_NAME));
    }

    @Test
    public void buildMailboxNameShouldAllowConversionFromPersonalPath() {
        when(imapSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        pathConverter = PathConverter.forSession(imapSession);

        assertThat(
            pathConverter.buildMailboxName(MailboxPath.forUser(BOB, MAILBOX_NAME)))
            .isEqualTo(
                DELEGATED_PREFIX + PATH_DELIMITER +
                    BOB + PATH_DELIMITER +
                    MAILBOX_NAME);
    }

    @Test
    public void buildMailboxNameShouldAllowConversionFromDelegatedPath() {
        when(imapSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        pathConverter = PathConverter.forSession(imapSession);

        assertThat(
            pathConverter.buildMailboxName(MailboxPath.forUser(USERNAME, MAILBOX_NAME)))
            .isEqualTo(
                PRIVATE_PREFIX + PATH_DELIMITER +
                    MAILBOX_NAME);
    }
}
