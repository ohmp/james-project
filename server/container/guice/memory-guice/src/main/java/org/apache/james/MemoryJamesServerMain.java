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

package org.apache.james;

import org.apache.james.jmap.JMAPModule;
import org.apache.james.jmap.MemoryMethodsModule;
import org.apache.james.modules.CommonServicesModule;
import org.apache.james.modules.data.MemoryDataModule;
import org.apache.james.modules.mailbox.MemoryMailboxModule;
import org.apache.james.modules.protocols.IMAPServerModule;
import org.apache.james.modules.protocols.JMAPServerModule;
import org.apache.james.modules.protocols.LMTPServerModule;
import org.apache.james.modules.protocols.ManageSieveServerModule;
import org.apache.james.modules.protocols.POP3ServerModule;
import org.apache.james.modules.protocols.ProtocolHandlerModule;
import org.apache.james.modules.protocols.SMTPServerModule;
import org.apache.james.modules.server.ActiveMQQueueModule;
import org.apache.james.modules.server.CamelMailetContainerModule;
import org.apache.james.modules.server.ConfigurationProviderModule;
import org.apache.james.modules.server.DNSServiceModule;
import org.apache.james.modules.server.MemoryDequeueDecoratorModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.modules.server.MailStoreRepositoryModule;
import org.apache.james.modules.server.MemoryMailQueueModule;
import org.apache.james.modules.server.QuotaModule;
import org.apache.james.modules.server.SieveModule;
import org.apache.onami.lifecycle.jsr250.PreDestroyModule;

import com.google.inject.Module;
import com.google.inject.util.Modules;

public class MemoryJamesServerMain {

    public static final Module memoryModule = Modules.combine(
        new CommonServicesModule(),
        new MemoryMailboxModule(),
        new MemoryDataModule(),
        new DNSServiceModule(),
        new IMAPServerModule(),
        new ProtocolHandlerModule(),
        new POP3ServerModule(),
        new SMTPServerModule(),
        new LMTPServerModule(),
        new ManageSieveServerModule(),
        new MemoryMailQueueModule(),
        new MemoryDequeueDecoratorModule(),
        new SieveModule(),
        new MailStoreRepositoryModule(),
        new CamelMailetContainerModule(),
        new QuotaModule(),
        new ConfigurationProviderModule(),
        new JMAPServerModule(new JMAPModule(new MemoryMethodsModule())),
        new PreDestroyModule());

    public static void main(String[] args) throws Exception {
        GuiceJamesServer server = new GuiceJamesServer(Modules.combine(
                memoryModule,
                new JMXServerModule()));
        server.start();
    }

}
