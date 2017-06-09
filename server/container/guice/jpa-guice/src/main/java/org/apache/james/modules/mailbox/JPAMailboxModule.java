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
package org.apache.james.modules.mailbox;

import java.util.HashMap;

import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.JPAConfiguration;
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthenticator;
import org.apache.james.adapter.mailbox.store.UserRepositoryAuthorizator;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.jpa.openjpa.OpenJPAMailboxManager;
import org.apache.james.mailbox.maildir.MaildirId;
import org.apache.james.mailbox.maildir.MaildirMailboxSessionMapperFactory;
import org.apache.james.mailbox.maildir.MaildirStore;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.Authorizator;
import org.apache.james.mailbox.store.JVMMailboxPathLocker;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailbox.store.mail.AttachmentMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.DefaultMessageId;
import org.apache.james.mailbox.store.user.SubscriptionMapperFactory;
import org.apache.james.modules.Names;
import org.apache.james.utils.MailboxManagerDefinition;
import org.apache.james.utils.PropertiesProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

public class JPAMailboxModule extends AbstractModule {

    private static final String MAILDIR_HOME = "maildir";

    @Override
    protected void configure() {
        install(new DefaultQuotaModule());
        install(new DefaultEventModule());

        bind(MaildirMailboxSessionMapperFactory.class).in(Scopes.SINGLETON);
        bind(JVMMailboxPathLocker.class).in(Scopes.SINGLETON);
        bind(StoreSubscriptionManager.class).in(Scopes.SINGLETON);
        bind(UserRepositoryAuthenticator.class).in(Scopes.SINGLETON);
        bind(UserRepositoryAuthorizator.class).in(Scopes.SINGLETON);
        bind(MaildirId.Factory.class).in(Scopes.SINGLETON);
        bind(SimpleGroupMembershipResolver.class).in(Scopes.SINGLETON);
        bind(UnionMailboxACLResolver.class).in(Scopes.SINGLETON);
        bind(DefaultMessageId.Factory.class).in(Scopes.SINGLETON);

        bind(MessageMapperFactory.class).to(MaildirMailboxSessionMapperFactory.class);
        bind(MailboxMapperFactory.class).to(MaildirMailboxSessionMapperFactory.class);
        bind(AttachmentMapperFactory.class).to(MaildirMailboxSessionMapperFactory.class);
        bind(MailboxSessionMapperFactory.class).to(MaildirMailboxSessionMapperFactory.class);
        bind(SubscriptionMapperFactory.class).to(MaildirMailboxSessionMapperFactory.class);
        bind(MessageId.Factory.class).to(DefaultMessageId.Factory.class);

        bind(SubscriptionManager.class).to(StoreSubscriptionManager.class);
        bind(MailboxPathLocker.class).to(JVMMailboxPathLocker.class);
        bind(Authenticator.class).to(UserRepositoryAuthenticator.class);
        bind(MailboxManager.class).to(StoreMailboxManager.class);
        bind(Authorizator.class).to(UserRepositoryAuthorizator.class);
        bind(MailboxId.Factory.class).to(MaildirId.Factory.class);
        bind(GroupMembershipResolver.class).to(SimpleGroupMembershipResolver.class);
        bind(MailboxACLResolver.class).to(UnionMailboxACLResolver.class);
        
        Multibinder.newSetBinder(binder(), MailboxManagerDefinition.class).addBinding().to(MaildirMailboxManagerDefinition.class);
    }

    @Provides @Named(Names.MAILBOXMANAGER_NAME) @Singleton
    public MailboxManager provideMailboxManager(StoreMailboxManager maildirMailboxManager) throws MailboxException {
        maildirMailboxManager.init();
        return maildirMailboxManager;
    }
    
    @Singleton
    private static class MaildirMailboxManagerDefinition extends MailboxManagerDefinition {
        @Inject
        private MaildirMailboxManagerDefinition(OpenJPAMailboxManager manager) {
            super("maildir-mailboxmanager", manager);
        }
    }

    @Provides
    @Singleton
    public EntityManagerFactory provideEntityManagerFactory(JPAConfiguration jpaConfiguration) {
        HashMap<String, String> properties = new HashMap<String, String>();
        
        properties.put("openjpa.ConnectionDriverName", jpaConfiguration.getDriverName());
        properties.put("openjpa.ConnectionURL", jpaConfiguration.getDriverURL());

        return Persistence.createEntityManagerFactory("Global", properties);

    }

    @Provides
    @Singleton
    public MaildirStore pStore(MailboxPathLocker locker) {
        return new MaildirStore(MAILDIR_HOME + "/%user", locker);
    }

    @Provides
    @Singleton
    JPAConfiguration provideConfiguration(PropertiesProvider propertiesProvider) throws Exception{
        PropertiesConfiguration dataSource = propertiesProvider.getConfiguration("james-database");
        return JPAConfiguration.builder()
                .driverName(dataSource.getString("database.driverClassName"))
                .driverURL(dataSource.getString("database.url"))
                .build();
    }

    @Provides
    @Singleton
    public UidProvider pUidP(MaildirStore store) {
        return store;
    }

    @Provides
    @Singleton
    public ModSeqProvider pModSeqP(MaildirStore store) {
        return store;
    }
}