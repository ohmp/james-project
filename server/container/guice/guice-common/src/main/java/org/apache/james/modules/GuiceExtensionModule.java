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

package org.apache.james.modules;

import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.james.utils.ExtendedClassLoader;
import org.apache.james.utils.ExtensionConfigurationPerformer;
import org.apache.james.utils.ExtensionGuiceProbe;
import org.apache.james.utils.GuiceGenericLoader;
import org.apache.james.utils.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Splitter;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;

public class GuiceExtensionModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceExtensionModule.class);
    public static final String DEFAULT_PACKAGE_NAME = "org.apache.james.modules";

    public static class ExtensionModule extends AbstractModule {
        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), ExtensionGuiceProbe.class);
            Multibinder.newSetBinder(binder(), ExtensionConfigurationPerformer.class);
        }
    }

    private final PropertiesProvider propertiesProvider;
    private final GuiceGenericLoader<Module> genericLoader;

    @Inject
    public GuiceExtensionModule(Injector injector, PropertiesProvider propertiesProvider, ExtendedClassLoader extendedClassLoader) {
        this.propertiesProvider = propertiesProvider;
        this.genericLoader = new GuiceGenericLoader<>(injector, extendedClassLoader, DEFAULT_PACKAGE_NAME);
    }

    public Module getConfiguredModule() throws Exception {
        try {
            PropertiesConfiguration configuration = propertiesProvider.getConfiguration("guice.extensions");

            List<String> extensionNames = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .splitToList(configuration.getString("extension.names", ""));

            return Modules.combine(
                Modules.combine(extensionNames.stream()
                    .map(Throwing.function(name -> configuration.getString(name + ".class")))
                    .map(Throwing.function(genericLoader::instanciate))
                    .collect(Guavate.toImmutableList())),
                new ExtensionModule());
        } catch (FileNotFoundException e) {
            LOGGER.warn("Missing guice.extensions.propoerties file. No registered guice extensions.");
            return Modules.combine();
        }
    }
}
