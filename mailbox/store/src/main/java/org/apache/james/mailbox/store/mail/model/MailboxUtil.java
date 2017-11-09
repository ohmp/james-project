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
package org.apache.james.mailbox.store.mail.model;

import java.util.Objects;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.PathDelimiter;

public class MailboxUtil {

    public static boolean isMailboxChildOf(Mailbox mailbox, Mailbox potentialParent, MailboxSession mailboxSession) {
        return isMailboxChildOf(mailbox, potentialParent, mailboxSession.getPathDelimiter());
    }

    public static boolean isMailboxChildOf(Mailbox mailbox, Mailbox potentialParent, PathDelimiter delimiter) {
        return Objects.equals(mailbox.getNamespace(), potentialParent.getNamespace())
            && Objects.equals(mailbox.getUser(), potentialParent.getUser())
            && isChildren(potentialParent.getName(), mailbox.getName(), delimiter);
    }

    public static boolean isChildren(String potentialParent, String potentialChild, PathDelimiter pathDelimiter) {
        return pathDelimiter.getHierarchyLevels(potentialChild)
            .filter(s -> !s.equals(potentialChild))
            .anyMatch(potentialParent::equals);
    }
}
