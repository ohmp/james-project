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

package org.apache.james.mailbox.maildir;

import java.io.IOException;

import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.MailboxManagerOptions;
import org.apache.james.mailbox.store.StoreMailboxAnnotationManager;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreRightManager;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.junit.rules.TemporaryFolder;

public class MaildirMailboxManagerProvider {
    public static StoreMailboxManager createMailboxManager(String configuration, TemporaryFolder temporaryFolder,
                                                           MailboxManagerOptions options) throws MailboxException, IOException {
        MaildirStore store = new MaildirStore(temporaryFolder.newFolder().getPath() + configuration, new JVMMailboxPathLocker());
        MaildirMailboxSessionMapperFactory mf = new MaildirMailboxSessionMapperFactory(store);

        MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
        StoreRightManager storeRightManager = new StoreRightManager(mf, aclResolver, options.getGroupMembershipResolver());

        DefaultDelegatingMailboxListener delegatingListener = new DefaultDelegatingMailboxListener();
        MailboxEventDispatcher mailboxEventDispatcher = new MailboxEventDispatcher(delegatingListener);
        StoreMailboxAnnotationManager annotationManager = new StoreMailboxAnnotationManager(mf, storeRightManager);
        StoreMailboxManager manager = new StoreMailboxManager(mf,
            options.getAuthenticator(),
            options.getAuthorizator(),
            new JVMMailboxPathLocker(),
            options.getMessageParser(), new DefaultMessageId.Factory(), annotationManager,
            mailboxEventDispatcher, delegatingListener, storeRightManager,
            options.getReservedMailboxMatcher());
        manager.init();

        return manager;
    }
}
