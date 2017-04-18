package org.apache.james;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.utils.ConfigurationPerformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;
import com.google.inject.multibindings.Multibinder;

public class GuiceJamesServerTest {

    public static final ConfigurationPerformer THROWING_CONFIGURATION_PERFORMER = new ConfigurationPerformer() {
        @Override
        public void initModule() {
            throw new RuntimeException();
        }

        @Override
        public List<Class<? extends Configurable>> forClasses() {
            return ImmutableList.of();
        }
    };
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public MemoryJmapTestRule memoryJmapTestRule = new MemoryJmapTestRule();
    private GuiceJamesServer guiceJamesServer;


    @Before
    public void setUp() {
        guiceJamesServer = memoryJmapTestRule.jmapServer();
    }

    @After
    public void tearDown() {
        guiceJamesServer.stop();
    }

    @Test
    public void serverShouldBeStartedAfterCallingStart() throws Exception {
        guiceJamesServer.start();

        assertThat(guiceJamesServer.isStarted()).isTrue();
    }

    @Test
    public void serverShouldNotBeStartedAfterCallingStop() throws Exception {
        guiceJamesServer.start();

        guiceJamesServer.stop();

        assertThat(guiceJamesServer.isStarted()).isFalse();
    }

    @Test
    public void serverShouldNotBeStartedBeforeCallingStart() throws Exception {
        assertThat(guiceJamesServer.isStarted()).isFalse();
    }

    @Test
    public void serverShouldPropagateUncaughtConfigurationException() throws Exception {
        expectedException.expect(RuntimeException.class);

        GuiceJamesServer overwittenServer = null;

        try {
            overwittenServer = this.guiceJamesServer.overrideWith(
                binder -> Multibinder.newSetBinder(binder, ConfigurationPerformer.class).addBinding().toInstance(
                    new ConfigurationPerformer() {
                        @Override
                        public void initModule() {
                            throw new RuntimeException();
                        }

                        @Override
                        public List<Class<? extends Configurable>> forClasses() {
                            return ImmutableList.of();
                        }
                    }
                )
            );
            overwittenServer.start();
        } finally {
            if (overwittenServer != null) {
                overwittenServer.stop();
            }
        }
    }

    @Test
    public void serverShouldNotBeStartedOnUncaughtException() throws Exception {
        GuiceJamesServer overwittenServer = null;

        try {
            overwittenServer = this.guiceJamesServer.overrideWith(
                binder -> Multibinder.newSetBinder(binder, ConfigurationPerformer.class)
                    .addBinding()
                    .toInstance(THROWING_CONFIGURATION_PERFORMER));

            try {
                overwittenServer.start();
            } catch (RuntimeException e) {}

            assertThat(overwittenServer.isStarted()).isFalse();
        } finally {
            if (overwittenServer != null) {
                overwittenServer.stop();
            }
        }
    }
}
