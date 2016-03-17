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

package org.apache.james.modules.server;

import javax.jms.ConnectionFactory;

import org.apache.james.jmap.send.PostDequeueDecoratorFactory;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.queue.activemq.ActiveMQMailQueueFactory;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.MailQueueItemDecoratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class ActiveMQQueueModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQQueueModule.class);
    private static final Logger TIMELINE_LOGGER = LoggerFactory.getLogger("timeline");

    @Override
    protected void configure() {
        bind(MailQueueItemDecoratorFactory.class).to(new TypeLiteral<PostDequeueDecoratorFactory<CassandraId>>(){}).in(Singleton.class);
    }

    @Provides
    @Singleton
    ConnectionFactory provideEmbededActiveMQ(EmbeddedActiveMQ embeddedActiveMQ) {
        return embeddedActiveMQ.getConnectionFactory();
    }

    @Provides
    @Singleton
    public MailQueueFactory createActiveMailQueueFactory(ActiveMQMailQueueFactory activeMQMailQueueFactory) {
        TIMELINE_LOGGER.info("21 MailQueueFactory creation started");
        activeMQMailQueueFactory.setUseJMX(true);
        activeMQMailQueueFactory.setLog(LOGGER);
        activeMQMailQueueFactory.init();
        TIMELINE_LOGGER.info("21 MailQueueFactory creation done");
        return activeMQMailQueueFactory;
    }
}
