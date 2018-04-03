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

import java.util.List;

import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.utils.ExtensionConfigurationPerformer;
import org.apache.james.utils.ExtensionGuiceProbe;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ExtensionGuiceProbe.class)
            .addBinding()
            .to(AdditionalProbe.class);

        Multibinder.newSetBinder(binder(), ExtensionConfigurationPerformer.class)
            .addBinding()
            .to(AdditionalConfigurationPerformer.class);
    }

    @Singleton
    public static class AdditionalProbe implements ExtensionGuiceProbe {
        private boolean configured = false;

        public boolean isLoaded() {
            return true;
        }

        public boolean isConfigured() {
            return configured;
        }

        private void configure() {
            configured = true;
        }
    }

    public static class AdditionalConfigurationPerformer implements ExtensionConfigurationPerformer {
        private final AdditionalProbe additionalProbe;

        @Inject
        public AdditionalConfigurationPerformer(AdditionalProbe additionalProbe) {
            this.additionalProbe = additionalProbe;
        }

        @Override
        public void initModule() {
            additionalProbe.configure();
        }

        @Override
        public List<Class<? extends Configurable>> forClasses() {
            return ImmutableList.of();
        }
    }
}
