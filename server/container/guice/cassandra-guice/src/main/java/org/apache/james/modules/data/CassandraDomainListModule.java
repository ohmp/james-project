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
package org.apache.james.modules.data;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainListDAO;
import org.apache.james.domainlist.lib.DomainListConfiguration;
import org.apache.james.domainlist.lib.DomainListDAO;
import org.apache.james.domainlist.lib.DomainListImpl;
import org.apache.james.server.core.configuration.ConfigurationProvider;
import org.apache.james.utils.InitializationOperation;
import org.apache.james.utils.InitilizationOperationBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.ProvidesIntoSet;

public class CassandraDomainListModule extends AbstractModule {

    @Override
    public void configure() {
        TypeLiteral<DomainListImpl<CassandraDomainListDAO>> domailListTypeLiteral = new TypeLiteral<DomainListImpl<CassandraDomainListDAO>>() {};

        bind(domailListTypeLiteral).in(Scopes.SINGLETON);
        bind(CassandraDomainListDAO.class).in(Scopes.SINGLETON);
        bind(CassandraDomainListDAO.class).in(Scopes.SINGLETON);
        bind(DomainListDAO.class).to(CassandraDomainListDAO.class);
        bind(DomainList.class).to(domailListTypeLiteral);
        Multibinder.newSetBinder(binder(), CassandraModule.class).addBinding().toInstance(org.apache.james.domainlist.cassandra.CassandraDomainListModule.MODULE);
    }

    @Provides
    @Singleton
    public DomainListConfiguration provideDomainListConfiguration(ConfigurationProvider configurationProvider) throws ConfigurationException {
        return DomainListConfiguration.from(configurationProvider.getConfiguration("domainlist"));
    }

    @ProvidesIntoSet
    InitializationOperation configureDomainList(DomainListConfiguration configuration, DomainListImpl<CassandraDomainListDAO> cassandraDomainList) {
        return InitilizationOperationBuilder
            .forClass(DomainListImpl.class)
            .init(() -> cassandraDomainList.configure(configuration));
    }
}
