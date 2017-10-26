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

package org.apache.james.mailbox.elasticsearch;

import javax.inject.Inject;

import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJsonV1;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcherV1;

public class ElasticSearchComponentProvider {
    private final ElasticSearchSearcherV1 elasticSearchSearcherV1;
    private final MessageToElasticSearchJsonV1 messageToElasticSearchJsonV1;
    private final MailboxMappingFactoryV1 mailboxMappingFactoryV1;

    @Inject
    public ElasticSearchComponentProvider(ElasticSearchSearcherV1 elasticSearchSearcherV1,
                                          MessageToElasticSearchJsonV1 messageToElasticSearchJsonV1,
                                          MailboxMappingFactoryV1 mailboxMappingFactoryV1) {
        this.elasticSearchSearcherV1 = elasticSearchSearcherV1;
        this.messageToElasticSearchJsonV1 = messageToElasticSearchJsonV1;
        this.mailboxMappingFactoryV1 = mailboxMappingFactoryV1;
    }

    public ElasticSearchSearcher getSearcher(int version) {
        switch (version) {
            case 1:
                return elasticSearchSearcherV1;
            default:
                throw new IllegalArgumentException("No searcher registered for version " + version);
        }
    }

    public MessageToElasticSearchJson getMessageToElasticSearchJson(int version) {
        switch (version) {
            case 1:
                return messageToElasticSearchJsonV1;
            default:
                throw new IllegalArgumentException("No MessageToElasticSearchJson registered for version " + version);
        }
    }

    public MailboxMappingFactory getMailboxMappingFactory(int version) {
        switch (version) {
            case 1:
                return mailboxMappingFactoryV1;
            default:
                throw new IllegalArgumentException("No MailboxMappingFactory registered for version " + version);
        }
    }
}
