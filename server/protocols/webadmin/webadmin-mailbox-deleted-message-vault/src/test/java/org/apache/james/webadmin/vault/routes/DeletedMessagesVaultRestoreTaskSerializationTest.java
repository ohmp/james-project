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
package org.apache.james.webadmin.vault.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.apache.james.core.Username;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.server.task.json.TaskAdditionalInformationSerializationContract;
import org.apache.james.server.task.json.TaskSerializationContract;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.vault.dto.query.QueryTranslator;
import org.apache.james.vault.search.CriterionFactory;
import org.apache.james.vault.search.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeletedMessagesVaultRestoreTaskSerializationTest implements TaskSerializationContract, TaskAdditionalInformationSerializationContract {
    static final Instant TIMESTAMP = Instant.parse("2018-11-13T12:00:55Z");
    static final TestId.Factory MAILBOX_ID_FACTORY = new TestId.Factory();
    static final QueryTranslator QUERY_TRANSLATOR = new QueryTranslator(MAILBOX_ID_FACTORY);
    static final String USERNAME = "james";
    static final Username USERNAME_TO_RESTORE = Username.of(USERNAME);
    static final Query QUERY = Query.of(CriterionFactory.hasAttachment(true));

    RestoreService exportService;
    DeletedMessagesVaultRestoreTaskDTO.Factory factory;

    @BeforeEach
    void setUp() {
        exportService = mock(RestoreService.class);
        factory = new DeletedMessagesVaultRestoreTaskDTO.Factory(exportService, QUERY_TRANSLATOR);
    }

    @Override
    public String serializedAdditionalInformation() {
        return "{\"type\":\"deleted-messages-restore\", \"user\":\"james\",\"successfulRestoreCount\":42,\"errorRestoreCount\":10, \"timestamp\":\"2018-11-13T12:00:55Z\"}";
    }

    @Override
    public TaskExecutionDetails.AdditionalInformation additionalInformation() {
        return new DeletedMessagesVaultRestoreTask.AdditionalInformation(USERNAME_TO_RESTORE,42, 10, TIMESTAMP);
    }

    @Override
    public AdditionalInformationDTOModule additionalInformationDTOModule() {
        return DeletedMessagesVaultRestoreTaskAdditionalInformationDTO.MODULE;
    }

    @Override
    public String serializedTask() {
        return "{\"type\":\"deleted-messages-restore\"," +
            "\"userToRestore\":\"james\"," +
            "\"query\":{\"combinator\":\"and\",\"criteria\":[{\"fieldName\":\"hasAttachment\",\"operator\":\"equals\",\"value\":\"true\"}]}" +
            "}";
    }

    @Override
    public Task task() {
        return new DeletedMessagesVaultRestoreTask(exportService, USERNAME_TO_RESTORE, QUERY);
    }

    @Override
    public TaskDTOModule taskDtoModule() {
        return DeletedMessagesVaultRestoreTaskDTO.module(factory);
    }

    @Override
    public String[] comparisonFields() {
        return new String[0];
    }

    @Test
    void deleteMessagesVaultExportTaskShouldDeserializeExportQuery() throws Exception {
        Task deserializedTask = JsonTaskSerializer.of(taskDtoModule()).deserialize(serializedTask());

        DeletedMessagesVaultRestoreTask deserializedRestoreTask = (DeletedMessagesVaultRestoreTask) deserializedTask;
        assertThat(QUERY_TRANSLATOR.toDTO(deserializedRestoreTask.query)).isEqualTo(QUERY_TRANSLATOR.toDTO(QUERY));
    }
}