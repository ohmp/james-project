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

package org.apache.james.vault;

import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryProvider;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryStore;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryUrlStore;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.configuration.FileConfigurationProvider;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.junit.jupiter.api.BeforeEach;

import com.google.common.collect.Sets;

public class MailRepositoryBasedDeletedMessageVaultTest implements DeletedMessageVaultContract {

    private DeletedMessageVault testee;

    @BeforeEach
    void setUp() throws Exception {
        Configuration configuration = Configuration.builder()
            .workingDirectory("../")
            .configurationFromClasspath()
            .build();
        FileSystemImpl fileSystem = new FileSystemImpl(configuration.directories());

        MemoryMailRepositoryUrlStore urlStore = new MemoryMailRepositoryUrlStore();
        MemoryMailRepositoryStore mailRepositoryStore = new MemoryMailRepositoryStore(urlStore, Sets.newHashSet(new MemoryMailRepositoryProvider()));
        mailRepositoryStore.configure(new FileConfigurationProvider(fileSystem, configuration)
            .getConfiguration("mailrepositorystore"));
        mailRepositoryStore.init();

        testee = new MailRepositoryBasedDeletedMessageVault(
            mailRepositoryStore,
            new MailRepositoryBasedDeletedMessageVault.Configuration("memory://deletedMessages/vault/"),
            new MailAdapter(new InMemoryId.Factory(), new InMemoryMessageId.Factory()));
    }

    @Override
    public DeletedMessageVault getVault() {
        return testee;
    }
}