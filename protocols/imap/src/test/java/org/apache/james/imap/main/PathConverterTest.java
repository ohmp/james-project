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

    private static final String ALICE = "alice";
    private static final char PATH_DELIMITER = '.';
    public static final String BOB = "bob";
    public static final String CEDRIC_DOMAIN = "cedric@domain.com";
    public static final String DAVID = "david";
    public static final String DAVID_DOMAIN = "david@domain.com";
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

    private ImapSession aliceSession;
    private PathConverter alicePathConverter;
    private PathConverter cedricDomainPathConverter;

    @Before
    public void setUp() {
        aliceSession = mock(ImapSession.class);
        MailboxSession mailboxSession = mock(MailboxSession.class);
        MailboxSession.User user = mock(MailboxSession.User.class);
        when(aliceSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
            .thenReturn(mailboxSession);
        when(aliceSession.getNamespaceConfiguration()).thenReturn(new DefaultNamespaceConfiguration());
        when(mailboxSession.getUser()).thenReturn(user);
        when(mailboxSession.getPathDelimiter()).thenReturn(PATH_DELIMITER);
        when(user.getUserName()).thenReturn(ALICE);
        alicePathConverter = PathConverter.forSession(aliceSession);

        ImapSession cedricDomainSession = mock(ImapSession.class);
        MailboxSession cedricDomainMailboxSession = mock(MailboxSession.class);
        MailboxSession.User cedricDomainUser = mock(MailboxSession.User.class);
        when(cedricDomainSession.getAttribute(ImapSessionUtils.MAILBOX_SESSION_ATTRIBUTE_SESSION_KEY))
            .thenReturn(cedricDomainMailboxSession);
        when(cedricDomainSession.getNamespaceConfiguration()).thenReturn(new DefaultNamespaceConfiguration());
        when(cedricDomainMailboxSession.getUser()).thenReturn(cedricDomainUser);
        when(cedricDomainMailboxSession.getPathDelimiter()).thenReturn(PATH_DELIMITER);
        when(cedricDomainUser.getUserName()).thenReturn(CEDRIC_DOMAIN);
        cedricDomainPathConverter = PathConverter.forSession(cedricDomainSession);
    }

    @Test
    public void buildFullPathShouldThrowOnNull() {
        assertThatThrownBy(() -> alicePathConverter.buildFullPath(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildMailboxNameShouldThrowOnNull() {
        assertThatThrownBy(() -> alicePathConverter.buildMailboxName(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void buildFullPathShouldAcceptEmpty() throws MailboxNotFoundException {
        assertThat(alicePathConverter.buildFullPath(""))
            .isEqualTo(MailboxPath.forUser(ALICE, ""));
    }

    @Test
    public void buildFullPathShouldAcceptRelativeMailboxName() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(alicePathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(ALICE, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedMailbox() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(alicePathConverter.buildFullPath(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptSubFolder() throws MailboxNotFoundException {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(alicePathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(ALICE, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedSubFolder() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        assertThat(alicePathConverter.buildFullPath(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder))
            .isEqualTo(MailboxPath.forUser(BOB, mailboxName + PATH_DELIMITER + subFolder));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUserAndPathSeparator() throws MailboxNotFoundException {
        assertThat(alicePathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(BOB, ""));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUser() throws MailboxNotFoundException {
        assertThat(alicePathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB))
            .isEqualTo(MailboxPath.forUser(ALICE, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + BOB));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndPathDelimiter() throws MailboxNotFoundException {
        assertThat(alicePathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(ALICE, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxOnly() throws MailboxNotFoundException {
        assertThat(alicePathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE))
            .isEqualTo(MailboxPath.forUser(ALICE, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildMailboxNameShouldAcceptRelativeMailboxName() {
        String mailboxName = "mailboxName";
        assertThat(alicePathConverter.buildMailboxName(MailboxPath.forUser(ALICE, mailboxName)))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedMailbox() {
        String mailboxName = "mailboxName";
        String fullMailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER + mailboxName;
        assertThat(alicePathConverter.buildMailboxName(MailboxPath.forUser(BOB, mailboxName)))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedSubFolder() {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(alicePathConverter.buildMailboxName(MailboxPath.forUser(ALICE, mailboxName)))
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
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUserAndSeparator() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(ALICE, mailboxName);
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUser() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB;
        MailboxPath mailboxPath = MailboxPath.forUser(ALICE, mailboxName);
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndSeparator() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(ALICE, mailboxName);
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailbox() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE;
        MailboxPath mailboxPath = MailboxPath.forUser(ALICE, mailboxName);
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptEmptyDelegatedMailboxName() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + BOB;
        MailboxPath mailboxPath = MailboxPath.forUser(BOB, "");
        assertThat(alicePathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildFullPathShouldHandlePrivateNamespace() throws MailboxNotFoundException {
        when(aliceSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        alicePathConverter = PathConverter.forSession(aliceSession);

        assertThat(
            alicePathConverter.buildFullPath(PRIVATE_PREFIX + PATH_DELIMITER + MAILBOX_NAME))
            .isEqualTo(
                MailboxPath.forUser(ALICE, MAILBOX_NAME));
    }

    @Test
    public void buildFullPathShouldReturnNotFoundWhenOutOfAllNamespace() {
        when(aliceSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        alicePathConverter = PathConverter.forSession(aliceSession);

        assertThatThrownBy(() ->
            alicePathConverter.buildFullPath(MAILBOX_NAME))
            .isInstanceOf(MailboxNotFoundException.class);
    }

    @Test
    public void buildFullPathShouldReturnMailboxPathWhenUsingSpecificUserNamespace() throws MailboxNotFoundException {
        when(aliceSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        alicePathConverter = PathConverter.forSession(aliceSession);

        assertThat(
            alicePathConverter.buildFullPath(DELEGATED_PREFIX + PATH_DELIMITER +
                BOB + PATH_DELIMITER +
                MAILBOX_NAME))
            .isEqualTo(
                MailboxPath.forUser(BOB, MAILBOX_NAME));
    }

    @Test
    public void buildMailboxNameShouldAllowConversionFromPersonalPath() {
        when(aliceSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        alicePathConverter = PathConverter.forSession(aliceSession);

        assertThat(
            alicePathConverter.buildMailboxName(MailboxPath.forUser(BOB, MAILBOX_NAME)))
            .isEqualTo(
                DELEGATED_PREFIX + PATH_DELIMITER +
                    BOB + PATH_DELIMITER +
                    MAILBOX_NAME);
    }

    @Test
    public void buildMailboxNameShouldAllowConversionFromDelegatedPath() {
        when(aliceSession.getNamespaceConfiguration())
            .thenReturn(CUSTOM_NAMESPACE_CONFIGURATION);
        alicePathConverter = PathConverter.forSession(aliceSession);

        assertThat(
            alicePathConverter.buildMailboxName(MailboxPath.forUser(ALICE, MAILBOX_NAME)))
            .isEqualTo(
                PRIVATE_PREFIX + PATH_DELIMITER +
                    MAILBOX_NAME);
    }

    @Test
    public void buildFullPathShouldAcceptRelativeMailboxNameWhenVirtualHosting() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(cedricDomainPathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedMailboxWhenVirtualHosting() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + DAVID + PATH_DELIMITER + mailboxName))
            .isEqualTo(MailboxPath.forUser(DAVID_DOMAIN, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptSubFolderWhenVirtualHosting() throws MailboxNotFoundException {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(cedricDomainPathConverter.buildFullPath(mailboxName))
            .isEqualTo(MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName));
    }

    @Test
    public void buildFullPathShouldAcceptDelegatedSubFolderWhenVirtualHosting() throws MailboxNotFoundException {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + DAVID + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder))
            .isEqualTo(MailboxPath.forUser(DAVID_DOMAIN, mailboxName + PATH_DELIMITER + subFolder));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUserAndPathSeparatorWhenVirtualHosting() throws MailboxNotFoundException {
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + DAVID + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(DAVID_DOMAIN, ""));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndUserWhenVirtualHosting() throws MailboxNotFoundException {
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + DAVID))
            .isEqualTo(MailboxPath.forUser(CEDRIC_DOMAIN, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
                PATH_DELIMITER + DAVID));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxAndPathDelimiterWhenVirtualHosting() throws MailboxNotFoundException {
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER))
            .isEqualTo(MailboxPath.forUser(CEDRIC_DOMAIN, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildFullPathShouldSupportMailboxesWithDelegationVirtualMailboxOnlyWhenVirtualHosting() throws MailboxNotFoundException {
        assertThat(cedricDomainPathConverter.buildFullPath(
            DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE))
            .isEqualTo(MailboxPath.forUser(CEDRIC_DOMAIN, DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE));
    }

    @Test
    public void buildMailboxNameShouldAcceptRelativeMailboxNameWhenVirtualHosting() {
        String mailboxName = "mailboxName";
        assertThat(cedricDomainPathConverter.buildMailboxName(MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName)))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedMailboxWhenVirtualHosting() {
        String mailboxName = "mailboxName";
        String fullMailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER
            + DAVID + PATH_DELIMITER + mailboxName;
        assertThat(cedricDomainPathConverter.buildMailboxName(MailboxPath.forUser(DAVID_DOMAIN, mailboxName)))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptDelegatedSubFolderWhenVirtualHosting() {
        String mailboxName = "mailboxName" + PATH_DELIMITER + "subFolder";
        assertThat(cedricDomainPathConverter.buildMailboxName(MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName)))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptAbsoluteUserPathWithSubFolderWhenVirtualHosting() {
        String mailboxName = "mailboxName";
        String subFolder = "subFolder";
        String fullMailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE +
            PATH_DELIMITER + DAVID + PATH_DELIMITER + mailboxName +
            PATH_DELIMITER + subFolder;
        MailboxPath mailboxPath = MailboxPath.forUser(DAVID_DOMAIN, mailboxName + PATH_DELIMITER + subFolder);
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(fullMailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUserAndSeparatorWhenVirtualHosting() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + DAVID + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName);
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndUserWhenVirtualHosting() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + DAVID;
        MailboxPath mailboxPath = MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName);
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxAndSeparatorWhenVirtualHosting() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER;
        MailboxPath mailboxPath = MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName);
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldSupportVirtualDelegationMailboxCEDRIC_DOMAIN() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE;
        MailboxPath mailboxPath = MailboxPath.forUser(CEDRIC_DOMAIN, mailboxName);
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }

    @Test
    public void buildMailboxNameShouldAcceptEmptyDelegatedMailboxNameCEDRIC_DOMAIN() {
        String mailboxName = DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE + PATH_DELIMITER + DAVID;
        MailboxPath mailboxPath = MailboxPath.forUser(DAVID_DOMAIN, "");
        assertThat(cedricDomainPathConverter.buildMailboxName(mailboxPath))
            .isEqualTo(mailboxName);
    }
}
