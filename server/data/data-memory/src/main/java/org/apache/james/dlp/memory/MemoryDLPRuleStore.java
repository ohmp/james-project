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

package org.apache.james.dlp.memory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.core.Domain;
import org.apache.james.dlp.api.DLPRule;
import org.apache.james.dlp.api.DLPRuleId;
import org.apache.james.dlp.api.DLPRulesStore;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class MemoryDLPRuleStore implements DLPRulesStore {

    private static class Entry {
        private final DLPRuleId id;
        private final Optional<DLPRule> payload;

        public Entry(DLPRuleId id, Optional<DLPRule> payload) {
            this.id = id;
            this.payload = payload;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry entry = (Entry) o;

                return Objects.equals(this.id, entry.id);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(id);
        }
    }

    private final Multimap<Domain, Entry> entries;
    private final MemoryDLPRuleId.Factory factory;

    public MemoryDLPRuleStore(MemoryDLPRuleId.Factory factory) {
        this.factory = factory;
        entries = Multimaps.synchronizedMultimap(ArrayListMultimap.<Domain, Entry>create());
    }

    @Override
    public Map<DLPRuleId, DLPRule> retrieveRules(Domain domain) {
        return entries.get(domain)
            .stream()
            .filter(entry -> entry.payload.isPresent())
            .collect(Guavate.toImmutableMap(
                entry -> entry.id,
                entry -> entry.payload.get()));
    }

    @Override
    public Optional<DLPRule> retrieveRule(Domain domain, DLPRuleId dlpRuleId) {
        return entries.get(domain)
            .stream()
            .filter(entry -> entry.id.equals(dlpRuleId))
            .findAny()
            .flatMap(entry -> entry.payload);
    }

    @Override
    public DLPRuleId store(Domain domain, DLPRule rule) {
        DLPRuleId ruleId = factory.generate();
        entries.put(domain, new Entry(ruleId, Optional.of(rule)));
        return ruleId;
    }

    @Override
    public void update(Domain domain, DLPRuleId dlpRuleId, DLPRule newRule) {
        entries.put(domain, new Entry(dlpRuleId, Optional.of(newRule)));
    }

    @Override
    public void delete(Domain domain, DLPRuleId dlpRuleId) {
        entries.remove(domain, new Entry(dlpRuleId, Optional.empty()));
    }

    @Override
    public void clear(Domain domain) {
        entries.removeAll(domain);
    }
}
