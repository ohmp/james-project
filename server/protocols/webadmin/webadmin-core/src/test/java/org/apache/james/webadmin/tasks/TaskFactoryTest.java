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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spark.Request;

class TaskFactoryTest {
    static final Task TASK_1 = mock(Task.class);
    static final Task TASK_2 = mock(Task.class);
    static RegisteredTaskGenerator TASK_GENERATOR_1 = new RegisteredTaskGenerator() {
        @Override
        public TaskRegistrationKey registrationKey() {
            return TaskRegistrationKey.of("task1");
        }

        @Override
        public Task generate(Request request) {
            return TASK_1;
        }
    };
    static RegisteredTaskGenerator TASK_GENERATOR_2 = new RegisteredTaskGenerator() {
        @Override
        public TaskRegistrationKey registrationKey() {
            return TaskRegistrationKey.of("task2");
        }

        @Override
        public Task generate(Request request) {
            return TASK_2;
        }
    };

    Request request;
    TaskFactory taskFactory;
    TaskFactory singleTaskFactory;

    @BeforeEach
    void setUp() {
        request = mock(Request.class);
        taskFactory = TaskFactory.builder().tasks(TASK_GENERATOR_1, TASK_GENERATOR_2).build();
        singleTaskFactory = TaskFactory.builder().tasks(TASK_GENERATOR_1).build();
    }

    @Test
    void buildShouldThrowWhenNoTasks() {
        assertThatThrownBy(() -> TaskFactory.builder().build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateShouldThrowFormattedMessageWhenNoTaskParamAndSeveralOptions() {
        assertThatThrownBy(() -> taskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'task' query parameter is compulsory. Supported values are [task1, task2]");
    }

    @Test
    void generateShouldThrowFormattedMessageWhenNoTaskParam() {
        assertThatThrownBy(() -> singleTaskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'task' query parameter is compulsory. Supported values are [task1]");
    }

    @Test
    void generateShouldThrowWhenCustomParameterValueIsInvalid() {
        TaskFactory taskFactory = TaskFactory.builder()
            .parameterName("custom")
            .tasks(TASK_GENERATOR_1)
            .build();

        when(request.queryParams("custom")).thenReturn("unknown");

        assertThatThrownBy(() -> taskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid value supplied for 'custom': unknown. Supported values are [task1]");
    }

    @Test
    void generateShouldThrowWhenCustomParameterNotSpecified() {
        TaskFactory taskFactory = TaskFactory.builder()
            .parameterName("custom")
            .tasks(TASK_GENERATOR_1)
            .build();

        when(request.queryParams("task")).thenReturn("unknown");

        assertThatThrownBy(() -> taskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'custom' query parameter is compulsory. Supported values are [task1]");
    }

    @Test
    void generateShouldThrowFormattedMessageWhenUnknownTaskParamAndSeveralOptions() {
        when(request.queryParams("task")).thenReturn("unknown");

        assertThatThrownBy(() -> taskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid value supplied for 'task': unknown. Supported values are [task1, task2]");
    }

    @Test
    void generateShouldThrowFormattedMessageWhenUnknownTaskParam() {
        when(request.queryParams("task")).thenReturn("unknown");

        assertThatThrownBy(() -> singleTaskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid value supplied for 'task': unknown. Supported values are [task1]");
    }

    @Test
    void generateShouldThrowWhenEmptyTaskParam() {
        when(request.queryParams("task")).thenReturn("");

        assertThatThrownBy(() -> singleTaskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'task' query parameter cannot be empty or blank. Supported values are [task1]");
    }

    @Test
    void generateShouldThrowWhenBlankTaskParam() {
        when(request.queryParams("task")).thenReturn(" ");

        assertThatThrownBy(() -> singleTaskFactory.generate(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'task' query parameter cannot be empty or blank. Supported values are [task1]");
    }

    @Test
    void generateShouldCreateCorrespondingTask() throws Exception {
        when(request.queryParams("task")).thenReturn("task1");

        assertThat(singleTaskFactory.generate(request))
            .isSameAs(TASK_1);
    }

    @Test
    void generateShouldHandleCustomTaskParameter() throws Exception {
        TaskFactory taskFactory = TaskFactory.builder()
            .parameterName("custom")
            .tasks(TASK_GENERATOR_1)
            .build();

        when(request.queryParams("custom")).thenReturn("task1");

        assertThat(taskFactory.generate(request))
            .isSameAs(TASK_1);
    }
}