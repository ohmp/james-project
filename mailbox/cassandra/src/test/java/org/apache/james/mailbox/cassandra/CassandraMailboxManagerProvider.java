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

package org.apache.james.mailbox.cassandra;

import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.MailboxManagerOptions;
import org.apache.james.mailbox.store.NoMailboxPathLocker;
import org.apache.james.mailbox.store.StoreMailboxAnnotationManager;
import org.apache.james.mailbox.store.StoreRightManager;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.event.MailboxEventDispatcher;

import com.datastax.driver.core.Session;
import com.google.common.base.Throwables;

public class CassandraMailboxManagerProvider {
    public static CassandraMailboxManager provideMailboxManager(Session session,
                                                                CassandraTypesProvider cassandraTypesProvider,
                                                                MailboxManagerOptions options) {
        CassandraMessageId.Factory messageIdFactory = new CassandraMessageId.Factory();

        CassandraMailboxSessionMapperFactory mapperFactory = TestCassandraMailboxSessionMapperFactory.forTests(
            session,
            cassandraTypesProvider,
            messageIdFactory);

        MailboxACLResolver aclResolver = new UnionMailboxACLResolver();
        GroupMembershipResolver groupMembershipResolver = options.getGroupMembershipResolver();
        StoreRightManager storeRightManager = new StoreRightManager(mapperFactory, aclResolver, groupMembershipResolver);

        DefaultDelegatingMailboxListener delegatingMailboxListener = new DefaultDelegatingMailboxListener();
        MailboxEventDispatcher mailboxEventDispatcher = new MailboxEventDispatcher(delegatingMailboxListener);
        StoreMailboxAnnotationManager annotationManager = new StoreMailboxAnnotationManager(mapperFactory, storeRightManager,
            options.getLimitAnnotationCount(), options.getLimitAnnotationSize());

        CassandraMailboxManager manager = new CassandraMailboxManager(mapperFactory,
            options.getAuthenticator(),
            options.getAuthorizator(),
            new NoMailboxPathLocker(),
            options.getMessageParser(),
            messageIdFactory, mailboxEventDispatcher,
            delegatingMailboxListener,
            annotationManager, storeRightManager);
        try {
            manager.init();
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }

        return manager;
    }

}
