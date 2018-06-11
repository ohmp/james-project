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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.james.core.Domain;
import org.apache.james.dlp.api.DLPRule;
import org.apache.james.dlp.api.DLPRuleId;
import org.apache.james.dlp.api.DLPRulesStore;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class MemoryDLPRuleStore implements DLPRulesStore {

    private final Multimap<Domain, DLPRule> entries;

    public MemoryDLPRuleStore() {
        entries = ArrayListMultimap.create();
    }

    @Override
    public synchronized Map<DLPRuleId, DLPRule> retrieveRules(Domain domain) {
        AtomicInteger atomicInteger = new AtomicInteger();
        return entries.get(domain)
            .stream()
            .collect(Guavate.toImmutableMap(
                entry -> new DLPRuleId(atomicInteger.incrementAndGet()),
                Function.identity()));
    }

    @Override
    public void store(Domain domain, List<DLPRule> rules) {
        entries.putAll(domain, rules);
    }

    @Override
    public synchronized void clear(Domain domain) {
        entries.removeAll(domain);
    }
}
