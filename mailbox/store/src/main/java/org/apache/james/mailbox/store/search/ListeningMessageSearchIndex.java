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
package org.apache.james.mailbox.store.search;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;

/**
 * {@link MessageSearchIndex} which needs to get registered as global {@link MailboxListener} and so get
 * notified about message changes. This will then allow to update the underlying index.
 * 
 *
 */
public abstract class ListeningMessageSearchIndex implements MessageSearchIndex, MailboxListener, MessageIndexer {
    public static final int UNLIMITED = -1;
    private final MessageMapperFactory factory;
    private final ListeningIndexerDelegate listeningIndexerDelegate;

    public ListeningMessageSearchIndex(MessageMapperFactory factory) {
        this.factory = factory;
        this.listeningIndexerDelegate = new ListeningIndexerDelegate(this, factory);
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return ExecutionMode.ASYNCHRONOUS;
    }

    /**
     * Return the {@link MessageMapperFactory}
     * 
     * @return factory
     */
    protected MessageMapperFactory getFactory() {
        return factory;
    }

    /**
     * Process the {@link org.apache.james.mailbox.MailboxListener.Event} and update the index if
     * something relevant is received
     */
    @Override
    public void event(Event event) {
        listeningIndexerDelegate.event(event);
    }
}
