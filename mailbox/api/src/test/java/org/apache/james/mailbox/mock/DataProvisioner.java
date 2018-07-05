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
package org.apache.james.mailbox.mock;

import java.io.UnsupportedEncodingException;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;

public class DataProvisioner {
    
    /**
     * Number of Domains to be created in the Mailbox Manager.
     */
    public static final int DOMAIN_COUNT = 3;
    
    /**
     * Number of Users (with INBOX) to be created in the Mailbox Manager.
     */
    public static final int USER_COUNT = 3;
    
    /**
     * Number of Sub Mailboxes (mailbox in INBOX) to be created in the Mailbox Manager.
     */
    public static final int SUB_MAILBOXES_COUNT = 3;
    
    /**
     * Number of Sub Sub Mailboxes (mailbox in a mailbox under INBOX) to be created in the Mailbox Manager.
     */
    public static final int SUB_SUB_MAILBOXES_COUNT = 3;
    
    /**
     * The expected Mailboxes count calculated based on the feeded mails.
     */
    public static final int EXPECTED_MAILBOXES_COUNT = DOMAIN_COUNT * 
                     (USER_COUNT + // INBOX
                      USER_COUNT * SUB_MAILBOXES_COUNT + // INBOX.SUB_FOLDER
                      USER_COUNT * SUB_MAILBOXES_COUNT * SUB_SUB_MAILBOXES_COUNT);  // INBOX.SUB_FOLDER.SUBSUB_FOLDER
    
    /**
     * Number of Messages per Mailbox to be created in the Mailbox Manager.
     */
    public static final int MESSAGE_PER_MAILBOX_COUNT = 3;
    
    /**
     * Utility method to feed the Mailbox Manager with a number of 
     * mailboxes and messages per mailbox.
     */
    public static void feedMailboxManager(MailboxManager mailboxManager) throws MailboxException, UnsupportedEncodingException {
        for (int i = 0; i < DOMAIN_COUNT; i++) {
            for (int j = 0; j < USER_COUNT; j++) {
                String user = "user" + j + "@localhost" + i;

                provisionUser(mailboxManager, user);
            }
        }
    }

    private static void provisionUser(MailboxManager mailboxManager, String user) throws MailboxException, UnsupportedEncodingException {
        MailboxSession mailboxSession = mailboxManager.createSystemSession(user);
        createMailbox(mailboxManager, mailboxSession, MailboxPath.inbox(mailboxSession));

        for (int k = 0; k < SUB_MAILBOXES_COUNT; k++) {
            String subFolderName = MailboxConstants.INBOX + ".SUB_FOLDER_" + k;
            createMailbox(mailboxManager, mailboxSession, MailboxPath.forUser(user, subFolderName));

            for (int l = 0; l < SUB_SUB_MAILBOXES_COUNT; l++) {
                String subSubfolderName = subFolderName + ".SUBSUB_FOLDER_" + l;
                createMailbox(mailboxManager, mailboxSession, MailboxPath.forUser(user, subSubfolderName));
            }
        }
        mailboxManager.logout(mailboxSession, true);
    }

    private static void createMailbox(MailboxManager mailboxManager, MailboxSession mailboxSession, MailboxPath mailboxPath) throws MailboxException, UnsupportedEncodingException {
        mailboxManager.startProcessingRequest(mailboxSession);
        mailboxManager.createMailbox(mailboxPath, mailboxSession);
        MessageManager messageManager = mailboxManager.getMailbox(mailboxPath, mailboxSession);
        for (int j = 0; j < MESSAGE_PER_MAILBOX_COUNT; j++) {
            messageManager.appendMessage(
                MessageManager.AppendCommand.builder()
                    .recent()
                    .withFlags(new Flags(Flags.Flag.RECENT))
                    .build(MockMail.MAIL_TEXT_PLAIN),
                mailboxSession);
        }
        mailboxManager.endProcessingRequest(mailboxSession);
    }
    
}
