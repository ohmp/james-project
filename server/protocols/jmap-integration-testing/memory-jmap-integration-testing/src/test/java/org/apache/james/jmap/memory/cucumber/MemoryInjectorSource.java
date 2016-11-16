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

package org.apache.james.jmap.memory.cucumber;

import org.apache.james.MemoryJamesServer;
import org.apache.james.jmap.methods.integration.cucumber.MainStepdefs;
import org.apache.james.jmap.methods.integration.cucumber.UserStepdefs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

import cucumber.api.guice.CucumberModules;
import cucumber.runtime.java.guice.InjectorSource;

public class MemoryInjectorSource implements InjectorSource {

    @Override
    public Injector getInjector() {
        return Guice.createInjector(Stage.PRODUCTION, CucumberModules.SCENARIO, binder -> {
            binder.bind(new TypeLiteral<MainStepdefs<?>>() {}).to(new TypeLiteral<MainStepdefs<MemoryJamesServer>>() {});
            binder.bind(new TypeLiteral<UserStepdefs<?>>() {}).to(new TypeLiteral<UserStepdefs<MemoryJamesServer>>() {});
            binder.bind(new TypeLiteral<MainStepdefs>() {}).to(new TypeLiteral<MainStepdefs<MemoryJamesServer>>() {});
            binder.bind(new TypeLiteral<UserStepdefs>() {}).to(new TypeLiteral<UserStepdefs<MemoryJamesServer>>() {});
        });
    }
}
