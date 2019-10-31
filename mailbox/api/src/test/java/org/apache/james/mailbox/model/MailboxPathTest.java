/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.core.Username;
import org.apache.james.mailbox.DefaultMailboxes;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class MailboxPathTest {
    private static final Username USER = Username.of("user");

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(MailboxPath.class)
            .verify();
    }

    @Test
    public void getHierarchyLevelsShouldBeOrdered() {
        assertThat(MailboxPath.forUser(USER, "inbox.folder.subfolder")
            .getHierarchyLevels('.'))
            .containsExactly(
                MailboxPath.forUser(USER, "inbox"),
                MailboxPath.forUser(USER, "inbox.folder"),
                MailboxPath.forUser(USER, "inbox.folder.subfolder"));
    }

    @Test
    public void getHierarchyLevelsShouldReturnPathWhenOneLevel() {
        assertThat(MailboxPath.forUser(USER, "inbox")
            .getHierarchyLevels('.'))
            .containsExactly(
                MailboxPath.forUser(USER, "inbox"));
    }

    @Test
    public void getHierarchyLevelsShouldReturnPathWhenEmptyName() {
        assertThat(MailboxPath.forUser(USER, "")
            .getHierarchyLevels('.'))
            .containsExactly(
                MailboxPath.forUser(USER, ""));
    }

    @Test
    public void getHierarchyLevelsShouldReturnPathWhenNullName() {
        assertThat(MailboxPath.forUser(USER, null)
            .getHierarchyLevels('.'))
            .containsExactly(
                MailboxPath.forUser(USER, null));
    }

    @Test
    public void sanitizeShouldNotThrowOnNullMailboxName() {
        assertThat(MailboxPath.forUser(USER, null)
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, null));
    }

    @Test
    public void sanitizeShouldReturnEmptyWhenEmpty() {
        assertThat(MailboxPath.forUser(USER, "")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, ""));
    }

    @Test
    public void sanitizeShouldRemoveMaximumOneTrailingDelimiterWhenAlone() {
        assertThat(MailboxPath.forUser(USER, ".")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, ""));
    }

    @Test
    public void sanitizeShouldPreserveHeadingDelimiter() {
        assertThat(MailboxPath.forUser(USER, ".a")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, ".a"));
    }

    @Test
    public void sanitizeShouldRemoveTrailingDelimiter() {
        assertThat(MailboxPath.forUser(USER, "a.")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, "a"));
    }

    @Test
    public void sanitizeShouldRemoveMaximumOneTrailingDelimiter() {
        assertThat(MailboxPath.forUser(USER, "a..")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, "a."));
    }

    @Test
    public void sanitizeShouldPreserveRedundantDelimiters() {
        assertThat(MailboxPath.forUser(USER, "a..a")
            .sanitize('.'))
            .isEqualTo(
                MailboxPath.forUser(USER, "a..a"));
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeFalseIfSingleLevelPath() {
        assertThat(MailboxPath.forUser(USER, "a")
            .hasEmptyNameInHierarchy('.'))
            .isFalse();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeFalseIfNestedLevelWithNonEmptyNames() {
        assertThat(MailboxPath.forUser(USER, "a.b.c")
            .hasEmptyNameInHierarchy('.'))
            .isFalse();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfEmptyPath() {
        assertThat(MailboxPath.forUser(USER, "")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfPathWithTwoEmptyNames() {
        assertThat(MailboxPath.forUser(USER, ".")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfPathWithAnEmptyNameBetweenTwoNames() {
        assertThat(MailboxPath.forUser(USER, "a..b")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfPathWithHeadingEmptyNames() {
        assertThat(MailboxPath.forUser(USER, "..a")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfPathWithATrailingEmptyName() {
        assertThat(MailboxPath.forUser(USER, "a.")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void hasEmptyNameInHierarchyShouldBeTrueIfPathWithTrailingEmptyNames() {
        assertThat(MailboxPath.forUser(USER, "a..")
            .hasEmptyNameInHierarchy('.'))
            .isTrue();
    }

    @Test
    public void isInboxShouldReturnTrueWhenINBOX() {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, DefaultMailboxes.INBOX);
        assertThat(mailboxPath.isInbox()).isTrue();
    }

    @Test
    public void isInboxShouldReturnTrueWhenINBOXWithOtherCase() {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, "InBoX");
        assertThat(mailboxPath.isInbox()).isTrue();
    }

    @Test
    public void isInboxShouldReturnFalseWhenOtherThanInbox() {
        MailboxPath mailboxPath = new MailboxPath(MailboxConstants.USER_NAMESPACE, USER, DefaultMailboxes.ARCHIVE);
        assertThat(mailboxPath.isInbox()).isFalse();
    }
}
