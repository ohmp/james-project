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

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.modules.mailbox.ConfigurationComponent;
import org.apache.james.utils.PropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class BlobStoreChoosingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreChoosingConfiguration.class);

    public static final boolean CACHE_ENABLED = true;

    public enum BlobStoreImplName {
        CASSANDRA("cassandra"),
        OBJECTSTORAGE("objectstorage"),
        HYBRID("hybrid");

        static String supportedImplNames() {
            return Stream.of(BlobStoreImplName.values())
                .map(BlobStoreImplName::getName)
                .collect(Collectors.joining(", "));
        }

        static BlobStoreImplName from(String name) {
            return Stream.of(values())
                .filter(blobName -> blobName.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("%s is not a valid name of BlobStores, " +
                    "please use one of supported values in: %s", name, supportedImplNames())));
        }

        private final String name;

        BlobStoreImplName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static final String BLOBSTORE_IMPLEMENTATION_PROPERTY = "implementation";
    static final String CACHE_ENABLE_PROPERTY = "cache.enable";

    public static BlobStoreChoosingConfiguration readBlobStoreChoosingConfiguration(PropertiesProvider propertiesProvider) throws ConfigurationException {
        try {
            Configuration configuration = propertiesProvider.getConfigurations(ConfigurationComponent.NAMES);
            return BlobStoreChoosingConfiguration.from(configuration);
        } catch (FileNotFoundException e) {
            LOGGER.warn("Could not find " + ConfigurationComponent.NAME + " configuration file, using cassandra blobstore as the default");
            return BlobStoreChoosingConfiguration.cassandra();
        }
    }

    static BlobStoreChoosingConfiguration from(Configuration configuration) {
        BlobStoreImplName blobStoreImplName = Optional.ofNullable(configuration.getString(BLOBSTORE_IMPLEMENTATION_PROPERTY))
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::trim)
            .map(BlobStoreImplName::from)
            .orElseThrow(() -> new IllegalStateException(String.format("%s property is missing please use one of " +
                "supported values in: %s", BLOBSTORE_IMPLEMENTATION_PROPERTY, BlobStoreImplName.supportedImplNames())));

        boolean cacheEnabled = configuration.getBoolean(CACHE_ENABLE_PROPERTY, false);

        return new BlobStoreChoosingConfiguration(blobStoreImplName, cacheEnabled);
    }

    public static BlobStoreChoosingConfiguration cassandra() {
        return new BlobStoreChoosingConfiguration(BlobStoreImplName.CASSANDRA, !CACHE_ENABLED);
    }

    public static BlobStoreChoosingConfiguration objectStorage() {
        return new BlobStoreChoosingConfiguration(BlobStoreImplName.OBJECTSTORAGE, !CACHE_ENABLED);
    }

    public static BlobStoreChoosingConfiguration cachingEnabled() {
        return new BlobStoreChoosingConfiguration(BlobStoreImplName.OBJECTSTORAGE, CACHE_ENABLED);
    }

    public static BlobStoreChoosingConfiguration hybrid() {
        return new BlobStoreChoosingConfiguration(BlobStoreImplName.HYBRID, !CACHE_ENABLED);
    }

    private final BlobStoreImplName implementation;
    private final boolean cacheEnabled;

    BlobStoreChoosingConfiguration(BlobStoreImplName implementation, boolean cacheEnabled) {
        this.implementation = implementation;
        this.cacheEnabled = cacheEnabled;
    }

    BlobStoreImplName getImplementation() {
        return implementation;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof BlobStoreChoosingConfiguration) {
            BlobStoreChoosingConfiguration that = (BlobStoreChoosingConfiguration) o;

            return Objects.equals(this.implementation, that.implementation)
                && Objects.equals(this.cacheEnabled, that.cacheEnabled);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(implementation, cacheEnabled);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("implementation", implementation)
            .add("cacheEnabled", cacheEnabled)
            .toString();
    }
}
