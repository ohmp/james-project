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

package org.apache.james.dlp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.AbstractMap;

import org.apache.james.core.Domain;
import org.junit.jupiter.api.Test;

public interface DLPRuleStoreContract {

    DLPRuleId unknownId();

    DLPRule RULE = new DLPRule("explanation", "regex", new DLPRule.Targets(true, false, false));
    DLPRule RULE_2 = new DLPRule("explanation2", "regex2", new DLPRule.Targets(true, true, false));

    @Test
    default void retrieveRulesShouldReturnEmptyWhenNone(DLPRulesStore dlpRulesStore) {
        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .isEmpty();
    }

    @Test
    default void retrieveRuleShouldReturnEmptyWhenDoNotExist(DLPRulesStore dlpRulesStore) {
        assertThat(dlpRulesStore.retrieveRule(Domain.LOCALHOST, unknownId()))
            .isEmpty();
    }

    @Test
    default void storeShouldReturnUsedRuleId(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId = dlpRulesStore.store(Domain.LOCALHOST, RULE);

        assertThat(ruleId).isNotNull();
    }

    @Test
    default void retrieveRuleShouldReturnExistingEntry(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId = dlpRulesStore.store(Domain.LOCALHOST, RULE);

        assertThat(dlpRulesStore.retrieveRule(Domain.LOCALHOST, ruleId))
            .contains(RULE);
    }

    @Test
    default void retrieveRulesShouldReturnExistingEntries(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId1 = dlpRulesStore.store(Domain.LOCALHOST, RULE);
        DLPRuleId ruleId2 = dlpRulesStore.store(Domain.LOCALHOST, RULE_2);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId1, RULE),
                new AbstractMap.SimpleEntry<>(ruleId2, RULE_2));
    }

    @Test
    default void retrieveRulesShouldNotReturnEntriesOfOtherDomains(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId1 = dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(Domain.of("any.com"), RULE_2);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId1, RULE));
    }

    @Test
    default void clearShouldRemoveAllEnriesOfADomain(DLPRulesStore dlpRulesStore) {
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        dlpRulesStore.store(Domain.LOCALHOST, RULE_2);

        dlpRulesStore.clear(Domain.LOCALHOST);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .isEmpty();
    }

    @Test
    default void clearShouldNotFailWhenDomainDoNotExist(DLPRulesStore dlpRulesStore) {
        assertThatCode(() -> dlpRulesStore.clear(Domain.LOCALHOST))
            .doesNotThrowAnyException();
    }

    @Test
    default void clearShouldOnlyRemoveEnriesOfADomain(DLPRulesStore dlpRulesStore) {
        Domain otherDomain = Domain.of("any.com");
        dlpRulesStore.store(Domain.LOCALHOST, RULE);
        DLPRuleId ruleId2 = dlpRulesStore.store(otherDomain, RULE_2);

        dlpRulesStore.clear(Domain.LOCALHOST);

        assertThat(dlpRulesStore.retrieveRules(otherDomain))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId2, RULE_2));
    }

    @Test
    default void deleteShouldRemoveSpecifiedEntry(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId1 = dlpRulesStore.store(Domain.LOCALHOST, RULE);
        DLPRuleId ruleId2 = dlpRulesStore.store(Domain.LOCALHOST, RULE_2);

        dlpRulesStore.delete(Domain.LOCALHOST, ruleId1);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId2, RULE_2));
    }

    @Test
    default void deleteShouldNotFailWhenEntryDoNotExist(DLPRulesStore dlpRulesStore) {
        assertThatCode(() -> dlpRulesStore.delete(Domain.LOCALHOST, unknownId()))
            .doesNotThrowAnyException();
    }

    @Test
    default void updateShouldUpdateExistingRules(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId1 = dlpRulesStore.store(Domain.LOCALHOST, RULE);

        dlpRulesStore.update(Domain.LOCALHOST, ruleId1, RULE_2);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId1, RULE_2));
    }

    @Test
    default void updateShouldCreateExistingRules(DLPRulesStore dlpRulesStore) {
        DLPRuleId ruleId = unknownId();
        dlpRulesStore.update(Domain.LOCALHOST, ruleId, RULE_2);

        assertThat(dlpRulesStore.retrieveRules(Domain.LOCALHOST))
            .containsOnly(new AbstractMap.SimpleEntry<>(ruleId, RULE_2));
    }

}
