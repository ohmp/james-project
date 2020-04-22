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

package org.apache.james.modules.blobstore;

import static org.apache.james.modules.blobstore.BlobStoreChoosingModule.readBlobStoreChoosingConfiguration;

import java.util.stream.Stream;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.james.JamesServerMain;
import org.apache.james.modules.blobstore.BlobStoreChoosingModule.CacheDisabledModule;
import org.apache.james.modules.blobstore.BlobStoreChoosingModule.CacheEnabledModule;
import org.apache.james.modules.mailbox.CassandraCacheSessionModule;
import org.apache.james.utils.PropertiesProvider;

import com.google.inject.Module;

public class BlobStoreCacheConfiguredModulesSupplier implements JamesServerMain.ConfiguredModulesSupplier {
    @Override
    public Stream<Module> configuredModules(PropertiesProvider propertiesProvider) throws ConfigurationException {
        BlobStoreChoosingConfiguration blobStoreChoosingConfiguration = readBlobStoreChoosingConfiguration(propertiesProvider);

        if (blobStoreChoosingConfiguration.isCacheEnabled()) {
            return Stream.of(new CassandraCacheSessionModule(), new CacheEnabledModule());
        }
        return Stream.of(new CacheDisabledModule());
    }
}
