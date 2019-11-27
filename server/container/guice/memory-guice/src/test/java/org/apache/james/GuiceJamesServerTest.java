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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.apache.james.lifecycle.api.Startable;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.utils.InitializationOperation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;

class GuiceJamesServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceJamesServerTest.class);

    private static final int LIMIT_TO_10_MESSAGES = 10;

    private static JamesServerBuilder extensionBuilder() {
        return new JamesServerBuilder()
            .server(configuration -> GuiceJamesServer.forConfiguration(configuration)
                .combineWith(MemoryJamesServerMain.IN_MEMORY_SERVER_AGGREGATE_MODULE)
                .overrideWith(new TestJMAPServerModule(LIMIT_TO_10_MESSAGES)))
            .disableAutoStart();
    }

    @Nested
    class NormalBehaviour {
        @RegisterExtension
        JamesServerExtension jamesServerExtension = extensionBuilder().build();

        @Test
        void serverShouldBeStartedAfterCallingStart(GuiceJamesServer server) {
            server.start();

            assertThat(server.isStarted()).isTrue();
        }

        @Test
        void serverShouldNotBeStartedAfterCallingStop(GuiceJamesServer server) {
            server.start();

            server.stop();

            assertThat(server.isStarted()).isFalse();
        }

        @Test
        void serverShouldNotBeStartedBeforeCallingStart(GuiceJamesServer server) {
            assertThat(server.isStarted()).isFalse();
        }
    }

    static class Stoppable {
        private final AtomicBoolean stopped;

        Stoppable(AtomicBoolean stopped) {
            this.stopped = stopped;
        }

        @PreDestroy
        public void dispose() {
            stopped.set(true);
        }
    }

    static class ThrowingInitializationOperation implements InitializationOperation {
        private final Stoppable stoppable;

        @Inject
        ThrowingInitializationOperation(Stoppable stoppable) {
            this.stoppable = stoppable;
        }

        @Override
        public void initModule() {
            throw new RuntimeException();
        }

        @Override
        public Class<? extends Startable> forClass() {
            return Startable.class;
        }

    }

    @Nested
    class InitFailed {
        AtomicBoolean stopped = new AtomicBoolean(false);

        private final InitializationOperation throwingInitializationOperation = new InitializationOperation() {
            @Override
            public void initModule() {
                throw new RuntimeException();
            }

            @Override
            public Class<? extends Startable> forClass() {
                return Startable.class;
            }
        };

        @RegisterExtension
        JamesServerExtension jamesServerExtension = extensionBuilder()
            .overrideServerModule(binder -> binder.bind(Stoppable.class).toInstance(new Stoppable(stopped)))
            .overrideServerModule(binder -> Multibinder.newSetBinder(binder, InitializationOperation.class)
                .addBinding()
                .to(ThrowingInitializationOperation.class))
            .build();

        @Test
        void serverShouldNotPropagateUncaughtConfigurationException(GuiceJamesServer server) {
            assertThatCode(server::start)
                .doesNotThrowAnyException();
        }

        @Test
        void serverShouldStopWhenStartFail(GuiceJamesServer server) {
            server.start();

            assertThat(stopped.get()).isTrue();
        }

        @Test
        void serverShouldNotBeStartedOnUncaughtException(GuiceJamesServer server) {
            try {
                server.start();
            } catch (RuntimeException e) {
                LOGGER.info("Ignored expected exception", e);
            }

            assertThat(server.isStarted()).isFalse();
        }
    }
}
