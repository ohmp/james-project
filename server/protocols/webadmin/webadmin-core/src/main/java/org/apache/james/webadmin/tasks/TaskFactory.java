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

package org.apache.james.webadmin.tasks;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.james.task.Task;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.apache.james.webadmin.dto.TaskIdDto;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import spark.Request;
import spark.Response;
import spark.Route;

public class TaskFactory {
    private static final String DEFAULT_PARAMETER = "task";

    public static class Builder {
        private Optional<String> taskParameterName;
        private ImmutableSet.Builder<TaskGenerator> tasks;

        public Builder() {
            taskParameterName = Optional.empty();
            tasks = ImmutableSet.builder();
        }

        public Builder parameterName(String parameterName) {
            this.taskParameterName = Optional.of(parameterName);
            return this;
        }

        public Builder tasks(TaskGenerator... taskGenerators) {
            this.tasks.add(taskGenerators);
            return this;
        }

        public Builder tasks(Set<TaskGenerator> taskGenerators) {
            this.tasks.addAll(taskGenerators);
            return this;
        }

        public TaskFactory build() {
            ImmutableSet<TaskGenerator> taskGeneratos = tasks.build();
            Preconditions.checkState(!taskGeneratos.isEmpty());
            return new TaskFactory(
                taskParameterName.orElse(DEFAULT_PARAMETER),
                taskGeneratos
                    .stream()
                    .collect(Guavate.toImmutableMap(
                        TaskGenerator::registrationKey,
                        Function.identity())));
        }
    }

    private static class TaskRoute implements Route {
        private final TaskFactory taskFactory;
        private final TaskManager taskManager;

        TaskRoute(TaskFactory taskFactory, TaskManager taskManager) {
            this.taskFactory = taskFactory;
            this.taskManager = taskManager;
        }

        @Override
        public Object handle(Request request, Response response) throws Exception {
            Task task = taskFactory.generate(request);
            TaskId taskId = taskManager.submit(task);
            return TaskIdDto.respond(response, taskId);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String taskParameterName;
    private final Map<TaskRegistrationKey, TaskGenerator> taskGenerators;

    private TaskFactory(String taskParameterName, Map<TaskRegistrationKey, TaskGenerator> taskGenerators) {
        this.taskParameterName = taskParameterName;
        this.taskGenerators = taskGenerators;
    }

    public Task generate(Request request) throws Exception {
        TaskRegistrationKey registrationKey = parseRegistrationKey(request);
        return Optional.ofNullable(taskGenerators.get(registrationKey))
            .map(Throwing.<TaskGenerator, Task>function(taskGenerator -> taskGenerator.generate(request)).sneakyThrow())
            .orElseThrow(() -> new IllegalArgumentException("Invalid value supplied for '" + taskParameterName + "': " + registrationKey.asString()
                + ". " + supportedValueMessage()));
    }

    public Route asRoute(TaskManager taskManager) {
        return new TaskRoute(this, taskManager);
    }

    private TaskRegistrationKey parseRegistrationKey(Request request) {
        return Optional.ofNullable(request.queryParams(taskParameterName))
            .map(this::validateParameter)
            .map(TaskRegistrationKey::of)
            .orElseThrow(() -> new IllegalArgumentException("'" + taskParameterName + "' query parameter is compulsory. " + supportedValueMessage()));
    }

    private String validateParameter(String parameter) {
        if (StringUtils.isBlank(parameter)) {
            throw new IllegalArgumentException("'" + taskParameterName + "' query parameter cannot be empty or blank. " + supportedValueMessage());
        }
        return parameter;
    }

    private String supportedValueMessage() {
        ImmutableList<String> supportedTasks = taskGenerators.keySet()
            .stream()
            .map(TaskRegistrationKey::asString)
            .collect(Guavate.toImmutableList());
        return "Supported values are [" + Joiner.on(", ").join(supportedTasks) + "]";
    }
}
