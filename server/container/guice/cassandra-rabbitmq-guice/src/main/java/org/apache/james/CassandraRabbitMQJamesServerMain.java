/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james;

import static org.apache.james.CassandraJamesServerMain.REQUIRE_TASK_MANAGER_MODULE;
import static org.apache.james.modules.blobstore.BlobStoreChoosingConfiguration.readBlobStoreChoosingConfiguration;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.james.modules.DistributedTaskManagerModule;
import org.apache.james.modules.TaskSerializationModule;
import org.apache.james.modules.blobstore.BlobStoreCacheConfiguredModulesSupplier;
import org.apache.james.modules.blobstore.BlobStoreChoosingConfiguration;
import org.apache.james.modules.blobstore.ChoosingBlobStoreConfiguredModulesSupplier;
import org.apache.james.modules.event.RabbitMQEventBusModule;
import org.apache.james.modules.rabbitmq.RabbitMQModule;
import org.apache.james.modules.server.JMXServerModule;
import org.apache.james.server.core.configuration.Configuration;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.utils.PropertiesProvider;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class CassandraRabbitMQJamesServerMain implements JamesServerMain {
    public static final Module MODULES =
        Modules
            .override(Modules.combine(REQUIRE_TASK_MANAGER_MODULE, new DistributedTaskManagerModule()))
            .with(new RabbitMQModule(), new RabbitMQEventBusModule(), new TaskSerializationModule());

    public static void main(String[] args) throws Exception {
        Configuration configuration = Configuration.builder()
            .useWorkingDirectoryEnvProperty()
            .build();

        BlobStoreChoosingConfiguration blobStoreChoosingConfiguration = parseBlobStoreChoosingConfiguration(configuration);

        Module baseModule = baseModule(blobStoreChoosingConfiguration);

        JamesServerMain.main(configuration,
            ImmutableList.of(baseModule, new JMXServerModule()));
    }

    static BlobStoreChoosingConfiguration parseBlobStoreChoosingConfiguration(Configuration configuration) throws ConfigurationException {
        PropertiesProvider propertiesProvider = new PropertiesProvider(new FileSystemImpl(configuration.directories()), configuration);
        return readBlobStoreChoosingConfiguration(propertiesProvider);
    }

    public static Module baseModule(BlobStoreChoosingConfiguration blobStoreChoosingConfiguration) {
        return Modules.combine(ImmutableList.<Module>builder()
                .add(MODULES)
                .addAll(new BlobStoreCacheConfiguredModulesSupplier().configuredModules(blobStoreChoosingConfiguration))
                .addAll(new ChoosingBlobStoreConfiguredModulesSupplier().configuredModules(blobStoreChoosingConfiguration))
                .build());
    }
}
