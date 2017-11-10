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

import org.junit.Test;

import com.github.steveash.guavate.Guavate;

import nl.jqno.equalsverifier.EqualsVerifier;

public class NoPathDelimiterTest {
    @Test
    public void shouldMatchBeanContract() {
        EqualsVerifier.forClass(NoPathDelimiter.class)
            .verify();
    }

    @Test
    public void getLastPathPartShouldReturnFullMailboxName() throws Exception {
        String name = "mailbox";
        assertThat(NoPathDelimiter.SINGLETON.getLastPathPart(name))
            .isEqualTo(name);
    }

    @Test
    public void getLastPathPartShouldReturnFullMailboxNameWhenContainsDelimiter() throws Exception {
        String name = "mailbox\0name";
        assertThat(NoPathDelimiter.SINGLETON.getLastPathPart(name))
            .isEqualTo(name);
    }

    @Test
    public void getLastPathPartShouldReturnFullMailboxNameWhenEmpty() throws Exception {
        String name = "";
        assertThat(NoPathDelimiter.SINGLETON.getLastPathPart(name))
            .isEqualTo(name);
    }


    @Test
    public void getParentShouldReturnEmptyWhenTopLevelMailbox() throws Exception {
        assertThat(NoPathDelimiter.SINGLETON.getParent("mailbox"))
            .isEmpty();
    }

    @Test
    public void getParentShouldReturnEmptyWhenContainsDelimiter() throws Exception {
        String name = "inbox\0toto";
        assertThat(NoPathDelimiter.SINGLETON.getParent(name))
            .isEmpty();
    }

    @Test
    public void getParentShouldReturnEmptyWhenEmpty() throws Exception {
        String name = "";
        assertThat(NoPathDelimiter.SINGLETON.getParent(name))
            .isEmpty();
    }

    @Test
    public void getFirstPathPartShouldReturnFullMailboxName() throws Exception {
        String expected = "mailbox";
        String name = NoPathDelimiter.SINGLETON.getFirstPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldReturnFullMailboxNameWhenContainsDelimiter() throws Exception {
        String expected = "mailbox\0toto";
        String name = NoPathDelimiter.SINGLETON.getFirstPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void getFirstPathPartShouldReturnFullMailboxNameWhenEmpty() throws Exception {
        String expected = "";
        String name = NoPathDelimiter.SINGLETON.getFirstPathPart(expected);
        assertThat(name).isEqualTo(expected);
    }

    @Test
    public void splitShouldAcceptEmptyValue() {
        String mailboxName = "";
        assertThat(NoPathDelimiter.SINGLETON
            .split(mailboxName))
            .containsOnly("");
    }

    @Test
    public void splitShouldHandleDelimiterOnly() {
        String mailboxName = "\0";
        assertThat(NoPathDelimiter.SINGLETON
            .split(mailboxName))
            .containsOnly("\0");
    }

    @Test
    public void splitShouldAcceptSimpleName() {
        String mailboxName = "name";
        assertThat(NoPathDelimiter.SINGLETON
            .split(mailboxName))
            .containsOnly(mailboxName);
    }

    @Test
    public void splitShouldAcceptPath() {
        String mailboxName = "aa\0bb";
        assertThat(NoPathDelimiter.SINGLETON
            .split(mailboxName))
            .containsOnly("aa\0bb");
    }

    @Test
    public void joinShouldReturnEmptyWhenNone() {
        assertThat(NoPathDelimiter.SINGLETON
            .join())
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnEmptyWhenOnlyEmpty() {
        assertThat(NoPathDelimiter.SINGLETON
            .join(""))
            .isEqualTo("");
    }

    @Test
    public void joinShouldReturnSimpleNameWhenOnlySimpleName() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void joinWithEmptyStringAsLastPartShouldAppendDelimiterAtTheEnd() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("aa", ""))
            .isEqualTo("aa");
    }

    @Test
    public void joinShouldReturnConcatWhenDoubleEmptyNames() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("", ""))
            .isEqualTo("");
    }

    @Test
    public void joinWithEmptyStringAsFirstPartConcatEmptyString() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("", "aa"))
            .isEqualTo("aa");
    }

    @Test
    public void joinShouldReturnConcatenationWhenDoubleNames() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("aa", "bb"))
            .isEqualTo("aabb");
    }

    @Test
    public void joinShouldAllowDelimiterInParts() {
        assertThat(NoPathDelimiter.SINGLETON
            .join("a\0a", "bb"))
            .isEqualTo("a\0abb");
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenEmpty() {
        assertThat(NoPathDelimiter.SINGLETON
            .containsPathDelimiter(""))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenAbsent() {
        assertThat(NoPathDelimiter.SINGLETON
            .containsPathDelimiter("name"))
            .isFalse();
    }

    @Test
    public void containsPathDelimiterShouldReturnFalseWhenPresent() {
        assertThat(NoPathDelimiter.SINGLETON
            .containsPathDelimiter("na\0me"))
            .isFalse();
    }

    @Test
    public void appendDelimiterShouldReturnEmptyWhenEmpty() {
        assertThat(NoPathDelimiter.SINGLETON
            .appendDelimiter(""))
            .isEqualTo("");
    }

    @Test
    public void appendDelimiterShouldReturnValue() {
        assertThat(NoPathDelimiter.SINGLETON
            .appendDelimiter("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void appendDelimiterShouldAcceptValueSuffixedByDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .appendDelimiter("aa\0"))
            .isEqualTo("aa\0");
    }

    @Test
    public void removeTrailingDelimiterShouldAcceptEmpty() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeTrailingDelimiter(""))
            .isEqualTo("");
    }

    @Test
    public void removeTrailingDelimiterShouldDoNothingWhenNoDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeTrailingDelimiter("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeTrailingDelimiterShouldDoNothingWhenMiddleDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeTrailingDelimiter("a\0a"))
            .isEqualTo("a\0a");
    }

    @Test
    public void removeTrailingDelimiterShouldDoNothingWhenBeginningDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeTrailingDelimiter("\0aa"))
            .isEqualTo("\0aa");
    }

    @Test
    public void removeTrailingDelimiterShouldDoNothingWhenDelimiterAtTheEnd() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeTrailingDelimiter("aa\0"))
            .isEqualTo("aa\0");
    }

    @Test
    public void removeHeadingDelimiterShouldAcceptEmpty() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeHeadingDelimiter(""))
            .isEqualTo("");
    }

    @Test
    public void removeHeadingDelimiterShouldDoNothingWhenNoDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeHeadingDelimiter("aa"))
            .isEqualTo("aa");
    }

    @Test
    public void removeHeadingDelimiterShouldDoNothingWhenMiddleDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeHeadingDelimiter("a\0a"))
            .isEqualTo("a\0a");
    }

    @Test
    public void removeHeadingDelimiterShouldDoNothingWhenStartingByDelimiter() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeHeadingDelimiter("\0aa"))
            .isEqualTo("\0aa");
    }

    @Test
    public void removeHeadingDelimiterShouldDoNothingWhenDelimiterAtTheEnd() {
        assertThat(NoPathDelimiter.SINGLETON
            .removeHeadingDelimiter("aa\0"))
            .isEqualTo("aa\0");
    }

    @Test
    public void getHierarchyLevelsShouldReturnASingleLevel() {
        assertThat(NoPathDelimiter.SINGLETON.getHierarchyLevels("inbox\0folder\0subfolder")
            .collect(Guavate.toImmutableList()))
            .containsExactly(
                "inbox\0folder\0subfolder");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenOneLevel() {
        assertThat(NoPathDelimiter.SINGLETON.getHierarchyLevels("inbox")
            .collect(Guavate.toImmutableList()))
            .containsExactly("inbox");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenEmptyName() {
        assertThat(NoPathDelimiter.SINGLETON.getHierarchyLevels("")
            .collect(Guavate.toImmutableList()))
            .containsExactly("");
    }

    @Test
    public void getHierarchyLevelsShouldReturnNameWhenNullName() {
        assertThat(NoPathDelimiter.SINGLETON.getHierarchyLevels(null)
            .map(Optional::ofNullable)
            .collect(Guavate.toImmutableList()))
            .containsExactly(Optional.empty());
    }

    @Test
    public void toPatternShouldBeEmpty() {
        assertThat(NoPathDelimiter.SINGLETON.toPattern())
            .isEqualTo("");
    }

    @Test
    public void asStringShouldBeEmpty() {
        assertThat(NoPathDelimiter.SINGLETON.asString())
            .isEqualTo("");
    }

    @Test
    public void isUndefinedShouldReturnTrue() {
        assertThat(NoPathDelimiter.SINGLETON.isUndefined())
            .isTrue();
    }
}