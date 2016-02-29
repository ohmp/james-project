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

package org.apache.james.modules.protocols;

import org.apache.james.jmap.JMAPModule;
import org.apache.james.jmap.JMAPServer;
import org.apache.james.jmap.crypto.JamesSignatureHandler;
import org.apache.james.utils.ConfigurationPerformer;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public class JMAPServerModule extends AbstractModule {

    private final JMAPModule jmapModule;

    public JMAPServerModule(JMAPModule jmapModule) {
        this.jmapModule = jmapModule;
    }

    public JMAPServerModule() {
        this(new JMAPModule());
    }

    @Override
    protected void configure() {
        install(jmapModule);
        Multibinder.newSetBinder(binder(), ConfigurationPerformer.class).addBinding().to(JMAPModuleConfigurationPerformer.class);
    }

    @Singleton
    public static class JMAPModuleConfigurationPerformer implements ConfigurationPerformer {

        private final JMAPServer server;
        private final JamesSignatureHandler signatureHandler;

        @Inject
        public JMAPModuleConfigurationPerformer(JMAPServer server, JamesSignatureHandler signatureHandler) {
            this.server = server;
            this.signatureHandler = signatureHandler;
        }

        @Override
        public void initModule() throws Exception {
            signatureHandler.init();
            server.configure(null);
            registerPEMWithSecurityProvider();
        }

        private void registerPEMWithSecurityProvider() {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

}
