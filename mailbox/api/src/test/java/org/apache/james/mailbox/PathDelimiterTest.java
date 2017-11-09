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

package org.apache.james.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.mailbox.model.MailboxConstants;
import org.junit.Test;

import com.github.steveash.guavate.Guavate;

import nl.jqno.equalsverifier.EqualsVerifier;

public class PathDelimiterTest {

    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(PathDelimiter.class)
            .verify();
    }

    @Test
    public void getLastPathPartShouldReturnMailboxNameWhenRootMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getLastPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getLastPathPartShouldReturnMailboxNameWhenChildMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getLastPathPart("inbox." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getLastPathPartShouldReturnMailboxNameWhenChildOfChildMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getLastPathPart("inbox.children." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getLastPathPartShouldAcceptEmptyChildren() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getLastPathPart("inbox.children." + expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getLastPathPartShouldAcceptEmpty() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getLastPathPart("");
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getParentShouldReturnEmptyWhenTopLevelMailbox() throws Exception {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getParent("mailbox"))
            .isEmpty();
    }

    @Test
    public void getParentShouldReturnEffectiveParent() throws Exception {
        String expected = "inbox";
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getParent(expected + ".mailbox"))
            .contains(expected);
    }

    @Test
    public void getParentShouldReturnEffectiveParentWhenParentIsNotTopLevel() throws Exception {
        String expected = "inbox.children";
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getParent(expected + ".mailbox"))
            .contains(expected);
    }

    @Test
    public void getParentPartShouldAcceptEmptyChildren() throws Exception {
        String expected = "";
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getParent(expected + ".mailbox"))
            .contains("");
    }

    @Test
    public void getLastPathPartShouldReturnEmptyOnEmptyString() throws Exception {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getParent(""))
            .isEmpty();
    }

    @Test
    public void getFirstPathPartShouldReturnMailboxNameWhenRootMailbox() throws Exception {
        String expected = "mailbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getFirstPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldReturnMailboxNameWhenChildMailbox() throws Exception {
        String expected = "inbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getFirstPathPart(expected + ".mailbox");
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldReturnMailboxNameWhenChildOfChildMailbox() throws Exception {
        String expected = "inbox";
        String name = MailboxConstants.DEFAULT_DELIMITER.getFirstPathPart(expected + ".children.mailbox");
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldAcceptEmptyChildren() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getFirstPathPart(expected + ".inbox.children");
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldAcceptEmpty() throws Exception {
        String expected = "";
        String name = MailboxConstants.DEFAULT_DELIMITER.getFirstPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void splitShouldAcceptEmptyValue() {
        String mailboxName = "";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("");
    }

    @Test
    public void splitShouldHandleSeparatorOnly() {
        String mailboxName = ".";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("", "");
    }

    @Test
    public void splitShouldAcceptSimpleName() {
        String mailboxName = "name";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly(mailboxName);
    }

    @Test
    public void splitShouldAcceptPath() {
        String mailboxName = "aa.bb";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "bb");
    }

    @Test
    public void splitShouldAcceptNamesStartingByDelimiter() {
        String mailboxName = ".aa";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("", "aa");
    }

    @Test
    public void splitShouldAcceptNamesFinishingByDelimiter() {
        String mailboxName = "aa.";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "");
    }

    @Test
    public void splitShouldAcceptNamesWithDoubleDelimiter() {
        String mailboxName = "aa..bb";
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .split(mailboxName))
            .containsOnly("aa", "", "bb");
    }

    @Test
    public void joinShouldReturnEmptyWhenNone() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join())
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnEmptyWhenOnlyEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join(""))
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnSimpleNameWhenOnlySimpleName() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void joinWithEmptyStringAsLastPartShouldAppendDelimiterAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa", ""))
            .isEqualTo("aa.");
    }

    @Test
    public void joinShouldReturnDelimiterWhenDoubleEmptyNames() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("", ""))
            .isEqualTo(".");
    }

    @Test
    public void joinWithEmptyStringAsFirstPartShouldAppendDelimiterAtTheBeginning() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("", "aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void joinShouldReturnConcatenationWhendoubleNames() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("aa", "bb"))
            .isEqualTo("aa.bb");
    }

    @Test
    public void joinShouldAllowDelimiterInParts() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .join("a.a", "bb"))
            .isEqualTo("a.a.bb");
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter(""))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenAbsent() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter("name"))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnTrueWhenPresent() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .containsPathDelimiter("na.me"))
            .isTrue();
    }

    @Test
    public void appendDelimiterShouldReturnDelimiterWhenEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter(""))
            .isEqualTo(".");
    }

    @Test
    public void appendDelimiterShouldConcatenateValueAndDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter("aa"))
            .isEqualTo("aa.");
    }

    @Test
    public void appendDelimiterShouldAcceptValueSuffixedByDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .appendDelimiter("aa."))
            .isEqualTo("aa..");
    }

    @Test
    public void removeTrailingSeparatorShouldAcceptEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator(""))
            .isEqualTo("");
    }

    @Test
    public void removeTrailingSeparatorShouldDoNothingWhenNoSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorShouldDoNothingWhenMiddleSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator("a.a"))
            .isEqualTo("a.a");
    }

    @Test
    public void removeTrailingSeparatorShouldDoNothingWhenBeginningSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator(".aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void removeTrailingSeparatorShouldReturnBaseNameWhenSeparatorAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator("aa."))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingSeparatorShouldRemoveSeparatorOnlyOnce() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeTrailingSeparator("aa.."))
            .isEqualTo("aa.");
    }

    @Test
    public void removeHeadingSeparatorShouldAcceptEmpty() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator(""))
            .isEqualTo("");
    }

    @Test
    public void removeHeadingSeparatorShouldDoNothingWhenNoSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeHeadingSeparatorShouldDoNothingWhenMiddleSeparator() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator("a.a"))
            .isEqualTo("a.a");
    }

    @Test
    public void removeHeadingSeparatorShouldReturnBaseNameWhenStartingByDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator(".aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeHeadingSeparatorShouldRemoveSeparatorOnlyOnce() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator("..aa"))
            .isEqualTo(".aa");
    }

    @Test
    public void removeHeadingSeparatorShouldDoNothingWhenSeparatorAtTheEnd() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER
            .removeHeadingSeparator("aa."))
            .isEqualTo("aa.");
    }

    @Test
    public void getHierarchyLevelsShouldBeOrdered() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getHierarchyLevels("inbox.folder.subfolder")
            .collect(Guavate.toImmutableList()))
            .containsExactly(
                "inbox",
                "inbox.folder",
                "inbox.folder.subfolder");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenOneLevel() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getHierarchyLevels("inbox")
            .collect(Guavate.toImmutableList()))
            .containsExactly(
                "inbox");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenEmptyName() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getHierarchyLevels("")
            .collect(Guavate.toImmutableList()))
            .containsExactly("");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenNullName() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.getHierarchyLevels(null)
            .map(Optional::ofNullable)
            .collect(Guavate.toImmutableList()))
            .containsExactly(Optional.empty());
    }

    @Test
    public void toPatternShouldQuoteDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.toPattern())
            .isEqualTo("\\Q.\\E");
    }

    @Test
    public void asStringShouldReturnStringValueOfDelimiter() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.asString())
            .isEqualTo(".");
    }

    @Test
    public void isUndefinedShouldReturnFalseWhenNotZero() {
        assertThat(MailboxConstants.DEFAULT_DELIMITER.isUndefined())
            .isFalse();
    }

    @Test
    public void isUndefinedShouldReturnTrueWhenZero() {
        assertThat(new PathDelimiter('\0').isUndefined())
            .isTrue();
    }
}