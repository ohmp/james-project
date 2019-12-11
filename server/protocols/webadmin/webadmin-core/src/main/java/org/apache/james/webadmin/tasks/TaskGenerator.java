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

import org.apache.james.task.Task;

import com.google.common.base.Preconditions;

import spark.Request;

public interface TaskGenerator {
    class Builder {
        @FunctionalInterface
        interface ToTask {
            Task generate(Request request) throws Exception;
        }

        @FunctionalInterface
        interface RequireRegistrationKey {
            RequireTask registrationKey(TaskRegistrationKey registrationKey);
        }

        @FunctionalInterface
        interface RequireTask {
            FinalStage task(ToTask task);
        }

        static class FinalStage {
            private final TaskRegistrationKey taskRegistrationKey;
            private final ToTask toTask;

            FinalStage(TaskRegistrationKey taskRegistrationKey, ToTask toTask) {
                Preconditions.checkNotNull(taskRegistrationKey);
                Preconditions.checkNotNull(toTask);

                this.taskRegistrationKey = taskRegistrationKey;
                this.toTask = toTask;
            }

            public TaskGenerator build() {
                return new TaskGenerator() {
                    @Override
                    public TaskRegistrationKey registrationKey() {
                        return taskRegistrationKey;
                    }

                    @Override
                    public Task generate(Request request) throws Exception {
                        return toTask.generate(request);
                    }
                };
            }
        }
    }

    static Builder.RequireRegistrationKey builder() {
        return registrationKey -> task -> new Builder.FinalStage(registrationKey, task);
    }

    TaskRegistrationKey registrationKey();

    Task generate(Request request) throws Exception;
}
