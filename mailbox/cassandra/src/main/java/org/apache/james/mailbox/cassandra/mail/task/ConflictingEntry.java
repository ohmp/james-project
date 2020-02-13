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

package org.apache.james.mailbox.cassandra.mail.task;

import java.util.Objects;

import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConflictingEntry {
    private final String mailboxPathAsString;
    private final String mailboxIdAsString;
    private final String pathRegistrationPathAsString;
    private final String pathRegistrationIdAsString;

    ConflictingEntry(MailboxPath mailboxPath,
                            MailboxId mailboxId,
                            MailboxPath pathRegistrationPath,
                            MailboxId pathRegistrationId) {
        this(mailboxPath.asString(), mailboxId.serialize(), pathRegistrationPath.asString(), pathRegistrationId.serialize());
    }

    public ConflictingEntry(@JsonProperty("mailboxPathAsString") String mailboxPathAsString,
                            @JsonProperty("mailboxIdAsString") String mailboxIdAsString,
                            @JsonProperty("pathRegistrationPathAsString") String pathRegistrationPathAsString,
                            @JsonProperty("pathRegistrationIdAsString") String pathRegistrationIdAsString) {
        this.mailboxPathAsString = mailboxPathAsString;
        this.mailboxIdAsString = mailboxIdAsString;
        this.pathRegistrationPathAsString = pathRegistrationPathAsString;
        this.pathRegistrationIdAsString = pathRegistrationIdAsString;
    }

    public String getMailboxPathAsString() {
        return mailboxPathAsString;
    }

    public String getMailboxIdAsString() {
        return mailboxIdAsString;
    }

    public String getPathRegistrationPathAsString() {
        return pathRegistrationPathAsString;
    }

    public String getPathRegistrationIdAsString() {
        return pathRegistrationIdAsString;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof ConflictingEntry) {
            ConflictingEntry that = (ConflictingEntry) o;

            return Objects.equals(this.mailboxPathAsString, that.mailboxPathAsString)
                && Objects.equals(this.mailboxIdAsString, that.mailboxIdAsString)
                && Objects.equals(this.pathRegistrationPathAsString, that.pathRegistrationPathAsString)
                && Objects.equals(this.pathRegistrationIdAsString, that.pathRegistrationIdAsString);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(mailboxPathAsString, mailboxIdAsString, pathRegistrationPathAsString, pathRegistrationIdAsString);
    }
}
