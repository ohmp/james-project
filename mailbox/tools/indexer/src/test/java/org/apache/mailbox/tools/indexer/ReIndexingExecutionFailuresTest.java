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

package org.apache.mailbox.tools.indexer;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import net.javacrumbs.jsonunit.core.Option;

class ReIndexingExecutionFailuresTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModules(new GuavaModule());
    }

    @Test
    void failuresShouldBeSerializedAsEmptyArrayWhenNone() throws Exception {
        ReIndexingExecutionFailures failures = new ReIndexingExecutionFailures(ImmutableList.of());

        assertThatJson(objectMapper.writeValueAsString(failures))
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("{}");
    }

    @Test
    void failuresShouldBeSerializedGroupedByMailboxId() throws Exception {
        ReIndexingExecutionFailures failures = new ReIndexingExecutionFailures(ImmutableList.of(
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(45), MessageUid.of(34)),
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(45), MessageUid.of(33)),
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(44), MessageUid.of(31)),
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(44), MessageUid.of(34)),
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(41), MessageUid.of(18)),
            new ReIndexingExecutionFailures.ReIndexingFailure(InMemoryId.of(16), MessageUid.of(24))));

        assertThatJson(objectMapper.writeValueAsString(failures))
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo("{" +
                "  \"45\":[" +
                "    {\"mailboxId\":\"45\",\"uid\":34}," +
                "    {\"mailboxId\":\"45\",\"uid\":33}]," +
                "  \"44\":[" +
                "    {\"mailboxId\":\"44\",\"uid\":31}," +
                "    {\"mailboxId\":\"44\",\"uid\":34}]," +
                "  \"41\":[{\"mailboxId\":\"41\",\"uid\":18}]," +
                "  \"16\":[{\"mailboxId\":\"16\",\"uid\":24}]" +
                "}");
    }

}