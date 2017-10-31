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

package org.apache.james.imap.main;

import java.util.List;

import org.apache.james.imap.api.ImapSessionUtils;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class PathConverter {

    public static final String DELEGATED_MAILBOXES_BASE = "Other users";
    private static final int BASE_PART = 0;
    private static final int USER_PART = 1;

    public static PathConverter forSession(ImapSession session) {
        return new PathConverter(session);
    }

    private final ImapSession session;

    private PathConverter(ImapSession session) {
        this.session = session;
    }

    public MailboxPath buildFullPath(String mailboxName) {
        Preconditions.checkNotNull(mailboxName);
        char pathDelimiter = ImapSessionUtils.getMailboxSession(session).getPathDelimiter();
        List<String> mailboxNameParts = Splitter.on(pathDelimiter)
            .splitToList(mailboxName);
        if (isADelegatedMailboxName(mailboxNameParts)) {
            return buildDelegatedMailboxPath(pathDelimiter, mailboxNameParts);
        }
        return buildPersonalMailboxPath(mailboxName);
    }

    private boolean isADelegatedMailboxName(List<String> mailboxNameParts) {
        return mailboxNameParts.size() > 2
            && mailboxNameParts.get(BASE_PART).equals(DELEGATED_MAILBOXES_BASE);
    }

    private MailboxPath buildDelegatedMailboxPath(char pathDelimiter, List<String> mailboxNameParts) {
        return new MailboxPath(MailboxConstants.USER_NAMESPACE,
            mailboxNameParts.get(USER_PART),
            sanitizeMailboxName(
                Joiner.on(pathDelimiter)
                    .skipNulls()
                    .join(Iterables.skip(mailboxNameParts, 2))));
    }

    private MailboxPath buildPersonalMailboxPath(String mailboxName) {
        return new MailboxPath(MailboxConstants.USER_NAMESPACE,
            ImapSessionUtils.getUserName(session),
            sanitizeMailboxName(mailboxName));
    }

    public String buildMailboxName(MailboxPath mailboxPath) {
        Preconditions.checkNotNull(mailboxPath);
        char pathDelimiter = ImapSessionUtils.getMailboxSession(session).getPathDelimiter();
        String userName = ImapSessionUtils.getUserName(session);
        if (userName.equals(mailboxPath.getUser())) {
            return mailboxPath.getName();
        }
        return DELEGATED_MAILBOXES_BASE +
            pathDelimiter +
            mailboxPath.getUser() +
            pathDelimiter +
            mailboxPath.getName();
    }

    private String sanitizeMailboxName(String mailboxName) {
        // use uppercase for INBOX
        // See IMAP-349
        if (mailboxName.equalsIgnoreCase(MailboxConstants.INBOX)) {
            return MailboxConstants.INBOX;
        }
        return mailboxName;
    }

}
