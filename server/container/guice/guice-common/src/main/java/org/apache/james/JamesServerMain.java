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

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.utils.PropertiesProvider;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public interface JamesServerMain {
    @FunctionalInterface
    interface ConfiguredModulesSupplier {
        Stream<Module> configuredModules(PropertiesProvider propertiesProvider) throws ConfigurationException;
    }

    static void main(Module... modules) throws Exception {
        Configuration configuration = Configuration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        GuiceJamesServer server = GuiceJamesServer.forConfiguration(configuration)
            .combineWith(modules);
        server.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    static void main(List<Module> baseModules, List<ConfiguredModulesSupplier> configuredModulesSuppliers) throws Exception {
        Configuration configuration = Configuration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        PropertiesProvider propertiesProvider = new PropertiesProvider(new FileSystemImpl(configuration.directories()), configuration);
        ImmutableList<Module> configuredModules = configuredModulesSuppliers.stream()
            .flatMap(Throwing.<ConfiguredModulesSupplier, Stream<Module>>function(
                configuredModulesSupplier -> configuredModulesSupplier.configuredModules(propertiesProvider)).sneakyThrow())
            .collect(Guavate.toImmutableList());

        GuiceJamesServer server = GuiceJamesServer.forConfiguration(configuration)
            .combineWith(Modules.combine(baseModules))
            .combineWith(Modules.combine(configuredModules));
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
