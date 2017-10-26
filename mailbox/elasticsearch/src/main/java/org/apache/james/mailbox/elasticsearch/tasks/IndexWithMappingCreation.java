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

package org.apache.james.mailbox.elasticsearch.tasks;

import javax.inject.Inject;

import org.apache.james.backends.es.IndexCreationFactory;
import org.apache.james.backends.es.NodeMappingFactory;
import org.apache.james.mailbox.elasticsearch.ElasticSearchComponentProvider;
import org.apache.james.mailbox.elasticsearch.MailboxElasticSearchConstants;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class IndexWithMappingCreation {

    private final Client client;
    private final ElasticSearchComponentProvider componentProvider;

    @Inject
    public IndexWithMappingCreation(Client client, ElasticSearchComponentProvider componentProvider) {
        this.client = client;
        this.componentProvider = componentProvider;
    }

    public void createIndexWithMapping(IndexCreationConfiguration indexCreationConfiguration) {
        new IndexCreationFactory()
            .useIndex(indexCreationConfiguration.getIndexName())
            .nbReplica(indexCreationConfiguration.getNbReplica())
            .nbShards(indexCreationConfiguration.getNbShards())
            .addAliases(indexCreationConfiguration.getAliasNames())
            .createIndexAndAliases(client);
        XContentBuilder mapping = componentProvider
            .getMailboxMappingFactory(indexCreationConfiguration.getSchemaVersion())
            .getMappingContent();
        NodeMappingFactory.applyMapping(client,
            indexCreationConfiguration.getIndexName(),
            MailboxElasticSearchConstants.MESSAGE_TYPE,
            mapping);
    }
}
