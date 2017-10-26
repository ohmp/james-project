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

package org.apache.james.mailbox.elasticsearch.events;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.james.backends.es.AliasName;
import org.apache.james.backends.es.DeleteByQueryPerformer;
import org.apache.james.backends.es.ElasticSearchIndexer;
import org.apache.james.backends.es.TypeName;
import org.apache.james.mailbox.elasticsearch.ElasticSearchComponentProvider;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.elasticsearch.client.Client;

public class ElasticSearchListeningMessageIndexerProvider {
    private final ElasticSearchComponentProvider elasticSearchComponentProvider;
    private final MessageMapperFactory messageMapperFactory;
    private final Client client;
    private final TypeName typeName;
    private final ExecutorService executor;

    @Inject
    public ElasticSearchListeningMessageIndexerProvider(ElasticSearchComponentProvider elasticSearchComponentProvider,
                                                        MessageMapperFactory messageMapperFactory, Client client,
                                                        TypeName typeName,
                                                        @Named("AsyncExecutor") ExecutorService executor) {
        this.elasticSearchComponentProvider = elasticSearchComponentProvider;
        this.messageMapperFactory = messageMapperFactory;
        this.client = client;
        this.typeName = typeName;
        this.executor = executor;
    }

    public ElasticSearchListeningMessageIndexer provide(AliasName aliasName, int version) {
        return new ElasticSearchListeningMessageIndexer(
            provideMessageIndexer(aliasName, version),
            messageMapperFactory);
    }

    public ElasticSearchMessageIndexer provideMessageIndexer(AliasName aliasName, int version) {
        DeleteByQueryPerformer deleteByQueryPerformer = new DeleteByQueryPerformer(client,
            executor,
            aliasName,
            typeName);
        ElasticSearchIndexer elasticSearchIndexer = new ElasticSearchIndexer(client,
            deleteByQueryPerformer,
            aliasName,
            typeName);
        return new ElasticSearchMessageIndexer(elasticSearchIndexer,
            elasticSearchComponentProvider.getMessageToElasticSearchJson(version));
    }

}
