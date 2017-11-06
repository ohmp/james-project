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

package org.apache.james.imap.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.protocols.imap.DefaultNamespaceConfiguration;
import org.junit.Before;
import org.junit.Test;

public class NamespaceReservedMailboxMatcherTest {

    private NamespaceReservedMailboxMatcher reservedMailboxMatcher;

    @Before
    public void setUp() {
        reservedMailboxMatcher = new NamespaceReservedMailboxMatcher(
            new DefaultNamespaceConfiguration());
    }

    @Test
    public void isReservedShouldReturnTrueWhenPersonalMailboxNamespace() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                DefaultNamespaceConfiguration.DEFAULT_PERSONAL_NAMESPACE,
                MailboxConstants.DEFAULT_DELIMITER))
            .isTrue();
    }

    @Test
    public void isReservedShouldReturnTrueWhenDelegatedMailboxNamespace() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE,
                MailboxConstants.DEFAULT_DELIMITER))
            .isTrue();
    }

    @Test
    public void isReservedShouldReturnTrueWhenDelegatedMailboxNamespaceWithEmptyUserName() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                MailboxConstants.DEFAULT_DELIMITER.join(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE,
                    ""),
                MailboxConstants.DEFAULT_DELIMITER))
            .isTrue();
    }

    @Test
    public void isReservedShouldReturnTrueWhenDelegatedMailboxNamespaceWithUserName() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                MailboxConstants.DEFAULT_DELIMITER.join(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE,
                    "boby"),
                MailboxConstants.DEFAULT_DELIMITER))
            .isTrue();
    }

    @Test
    public void isReservedShouldReturnFalseWhenPrefixedByDelegatedMailboxNamespace() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                MailboxConstants.DEFAULT_DELIMITER.join(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE
                    + "boby"),
                MailboxConstants.DEFAULT_DELIMITER))
            .isFalse();
    }

    @Test
    public void isReservedShouldReturnTrueWhenDelegatedMailbox() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                MailboxConstants.DEFAULT_DELIMITER.join(DefaultNamespaceConfiguration.DELEGATED_MAILBOXES_BASE,
                    "boby",
                    "mailbox"),
                MailboxConstants.DEFAULT_DELIMITER))
            .isTrue();
    }

    @Test
    public void isReservedShouldReturnFalseWhenPrivateMailbox() {
        assertThat(reservedMailboxMatcher
            .isReserved(
                "mailbox",
                MailboxConstants.DEFAULT_DELIMITER))
            .isFalse();
    }

}