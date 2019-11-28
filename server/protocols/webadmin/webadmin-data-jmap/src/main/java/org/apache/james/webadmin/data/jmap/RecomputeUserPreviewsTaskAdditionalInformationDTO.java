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

import java.time.Instant;

import org.apache.james.core.Username;
import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;

public class RecomputeUserPreviewsTaskAdditionalInformationDTO implements AdditionalInformationDTO {
    public static final AdditionalInformationDTOModule<RecomputeUserPreviewsTask.AdditionalInformation, RecomputeUserPreviewsTaskAdditionalInformationDTO> SERIALIZATION_MODULE =
        DTOModule.forDomainObject(RecomputeUserPreviewsTask.AdditionalInformation.class)
            .convertToDTO(RecomputeUserPreviewsTaskAdditionalInformationDTO.class)
            .toDomainObjectConverter(dto -> new RecomputeUserPreviewsTask.AdditionalInformation(
                Username.of(dto.username),
                dto.getProcessedMessageCount(),
                dto.getFailedMessageCount(),
                dto.timestamp))
            .toDTOConverter((details, type) -> new RecomputeUserPreviewsTaskAdditionalInformationDTO(
                type,
                details.timestamp(),
                details.getUsername(),
                details.getProcessedMessageCount(),
                details.getFailedMessageCount()))
            .typeName(RecomputeUserPreviewsTask.TASK_TYPE.asString())
            .withFactory(AdditionalInformationDTOModule::new);

    private final String type;
    private final Instant timestamp;
    private final String username;
    private final long processedMessageCount;
    private final long failedMessageCount;

    @VisibleForTesting
    RecomputeUserPreviewsTaskAdditionalInformationDTO(@JsonProperty("type") String type,
                                                      @JsonProperty("timestamp") Instant timestamp,
                                                      @JsonProperty("username") String username,
                                                      @JsonProperty("processedMessageCount") long processedMessageCount,
                                                      @JsonProperty("failedMessageCount") long failedMessageCount) {
        this.type = type;
        this.timestamp = timestamp;
        this.username = username;
        this.processedMessageCount = processedMessageCount;
        this.failedMessageCount = failedMessageCount;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public long getProcessedMessageCount() {
        return processedMessageCount;
    }

    public long getFailedMessageCount() {
        return failedMessageCount;
    }
}
