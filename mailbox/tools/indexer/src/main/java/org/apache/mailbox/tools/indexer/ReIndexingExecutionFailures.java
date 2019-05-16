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

import java.util.List;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.Multimap;

public class ReIndexingExecutionFailures {
    public static class ReIndexingFailure {
        private final MailboxId mailboxId;
        private final MessageUid uid;

        public ReIndexingFailure(MailboxId mailboxId, MessageUid uid) {
            this.mailboxId = mailboxId;
            this.uid = uid;
        }

        public String getMailboxId() {
            return mailboxId.serialize();
        }

        public long getUid() {
            return uid.asLong();
        }
    }

    private final List<ReIndexingFailure> failures;

    public ReIndexingExecutionFailures(List<ReIndexingFailure> failures) {
        this.failures = failures;
    }

    @JsonValue
    public Multimap<String, ReIndexingFailure> failures() {
        return failures.stream()
            .collect(Guavate.toImmutableListMultimap(ReIndexingFailure::getMailboxId));
    }
}
