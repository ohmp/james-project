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

import org.apache.james.backends.es.AliasName;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.elasticsearch.events.ElasticSearchListeningMessageIndexerProvider;
import org.apache.james.mailbox.store.mail.MailboxMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;

public class ElasticSearchReIndexerProvider {
    private final MailboxManager mailboxManager;
    private final MailboxMapperFactory mailboxMapperFactory;
    private final MessageMapperFactory messageMapperFactory;
    private final ElasticSearchListeningMessageIndexerProvider messageIndexerProvider;

    @Inject
    public ElasticSearchReIndexerProvider(MailboxManager mailboxManager,
                                          MailboxMapperFactory mailboxMapperFactory,
                                          MessageMapperFactory messageMapperFactory,
                                          ElasticSearchListeningMessageIndexerProvider messageIndexerProvider) {
        this.mailboxManager = mailboxManager;
        this.mailboxMapperFactory = mailboxMapperFactory;
        this.messageMapperFactory = messageMapperFactory;
        this.messageIndexerProvider = messageIndexerProvider;
    }

    public ElasticSearchReIndexer provideReindexer(AliasName aliasName, int version) {
        return new ElasticSearchReIndexer(
            mailboxManager,
            mailboxMapperFactory,
            messageMapperFactory,
            messageIndexerProvider.provideMessageIndexer(aliasName, version));
    }
}
