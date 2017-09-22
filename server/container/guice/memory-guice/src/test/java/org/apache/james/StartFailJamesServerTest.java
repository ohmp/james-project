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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import javax.inject.Inject;

import org.apache.james.modules.server.WebAdminServerModule;
import org.apache.james.webadmin.WebAdminServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartFailJamesServerTest {
    public static class FailingConfigurationPerformer extends WebAdminServerModule.WebAdminServerModuleConfigurationPerformer {
        @Inject
        public FailingConfigurationPerformer(WebAdminServer webAdminServer) {
            super(webAdminServer);
        }

        @Override
        public void initModule() {
            throw new RuntimeException();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StartFailJamesServerTest.class);
    private static final String JAMES_SERVER_HOST = "127.0.0.1";
    private static final int SMTP_PORT = 1025;


    @Rule
    public MemoryJmapTestRule memoryJmap = new MemoryJmapTestRule();

    private GuiceJamesServer guiceJamesServer;
    private SocketChannel socketChannel;


    @Before
    public void setUp() throws Exception {
        guiceJamesServer = memoryJmap.jmapServer()
            .overrideWith(binder -> binder.bind(WebAdminServerModule.WebAdminServerModuleConfigurationPerformer.class)
                .to(FailingConfigurationPerformer.class));

        socketChannel = SocketChannel.open();
    }

    @After
    public void clean() {
        try {
            guiceJamesServer.stop();
        } catch (Exception e) {
            // If test succeed James is expected to be already stopped
            LOGGER.warn("Error while stopping James server", e);
        }
    }

    @Test
    public void startShouldStopJamesServerWhenExceptionOnJamesInitialisation() throws Exception {
        assertThatThrownBy(() -> guiceJamesServer.start())
            .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, SMTP_PORT)))
            .isInstanceOf(IOException.class);
    }
}
