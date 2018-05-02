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

package org.apache.james.eventsourcing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

public class CommandDispatcher {

    public interface Command {
    }

    private final EventBus eventBus;
    private Map<Class, Function<Command, List<? extends Event>>> handlers;

    public CommandDispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
        this.handlers = new ConcurrentHashMap<>();
    }

    public <C extends Command, HandlerT extends Function<C, List<? extends Event>>>
        CommandDispatcher register(Class<C> type, HandlerT commandHandler) {

        handlers.put(type, (Function<Command, List<? extends Event>>) commandHandler);
        return this;
    }

    public void dispatch(Command c) {
        try {
            Optional<List<? extends Event>> events = Optional.ofNullable(handlers.get(c.getClass())).map(f -> f.apply(c));
            eventBus.publish(events.orElse(ImmutableList.of()));
        } catch (EventBus.EventStoreFailedException e) {
            //Retry
        }
    }
}
