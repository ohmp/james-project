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

package org.apache.james.webadmin.data.jmap;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.apache.james.core.Username;
import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.TaskDTO;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskType;

import com.fasterxml.jackson.annotation.JsonProperty;

import reactor.core.scheduler.Schedulers;

public class RecomputeUserPreviewsTask implements Task {
    static final TaskType TASK_TYPE = TaskType.of("RecomputeUserPreviewsTask");

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private static AdditionalInformation from(Username username, MessagePreviewCorrector.Context context) {
            return new AdditionalInformation(username,
                context.getProcessedMessageCount(),
                context.getFailedMessageCount(),
                Clock.systemUTC().instant());
        }

        private final Username username;
        private final long processedMessageCount;
        private final long failedMessageCount;
        private final Instant timestamp;

        public AdditionalInformation(Username username, long processedMessageCount, long failedMessageCount, Instant timestamp) {
            this.username = username;
            this.processedMessageCount = processedMessageCount;
            this.failedMessageCount = failedMessageCount;
            this.timestamp = timestamp;
        }

        public String getUsername() {
            return username.asString();
        }

        public long getProcessedMessageCount() {
            return processedMessageCount;
        }

        public long getFailedMessageCount() {
            return failedMessageCount;
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }
    }

    public static class RecomputeUserPreviewsTaskDTO implements TaskDTO {
        private final String type;
        private final String username;

        public RecomputeUserPreviewsTaskDTO(@JsonProperty("type") String type,
                                            @JsonProperty("username") String username) {
            this.type = type;
            this.username = username;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getUsername() {
            return username;
        }
    }

    public static TaskDTOModule<RecomputeUserPreviewsTask, RecomputeUserPreviewsTaskDTO> module(MessagePreviewCorrector corrector) {
        return DTOModule
            .forDomainObject(RecomputeUserPreviewsTask.class)
            .convertToDTO(RecomputeUserPreviewsTaskDTO.class)
            .toDomainObjectConverter(dto -> new RecomputeUserPreviewsTask(Username.of(dto.username), corrector))
            .toDTOConverter((task, type) -> new RecomputeUserPreviewsTaskDTO(type, task.username.asString()))
            .typeName(TASK_TYPE.asString())
            .withFactory(TaskDTOModule::new);
    }

    private final Username username;
    private final MessagePreviewCorrector corrector;
    private final MessagePreviewCorrector.Context context;

    RecomputeUserPreviewsTask(Username username, MessagePreviewCorrector corrector) {
        this.username = username;
        this.corrector = corrector;
        this.context = new MessagePreviewCorrector.Context();
    }

    @Override
    public Result run() {
        corrector.correctAllPreviews(context, username)
            .subscribeOn(Schedulers.boundedElastic())
            .block();

        if (context.noFailure()) {
            return Result.COMPLETED;
        }
        return Result.PARTIAL;
    }

    @Override
    public TaskType type() {
        return TASK_TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(AdditionalInformation.from(username, context));
    }
}