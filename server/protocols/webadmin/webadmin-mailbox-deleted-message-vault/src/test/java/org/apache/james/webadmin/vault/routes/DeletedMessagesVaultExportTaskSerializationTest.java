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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import javax.mail.internet.AddressException;

import org.apache.james.core.MailAddress;
import org.apache.james.core.Username;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationSerializer;
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
import org.apache.mailet.base.MailAddressFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeletedMessagesVaultExportTaskSerializationTest implements TaskSerializationContract, TaskAdditionalInformationSerializationContract {
    static final Instant TIMESTAMP = Instant.parse("2018-11-13T12:00:55Z");
    static final TestId.Factory MAILBOX_ID_FACTORY = new TestId.Factory();
    static final QueryTranslator QUERY_TRANSLATOR = new QueryTranslator(MAILBOX_ID_FACTORY);
    static final String USERNAME = "james";
    static final Username USERNAME_EXPORT_FROM = Username.of(USERNAME);
    static final Query QUERY = Query.of(CriterionFactory.hasAttachment(true));
    static final MailAddress EXPORT_TO = MailAddressFixture.ANY_AT_JAMES;

    ExportService exportService;
    DeletedMessagesVaultExportTaskDTO.Factory factory;

    @BeforeEach
    void setUp() {
        exportService = mock(ExportService.class);
        factory = new DeletedMessagesVaultExportTaskDTO.Factory(exportService, QUERY_TRANSLATOR);
    }

    @Override
    public String serializedAdditionalInformation() {
        return "{\"type\":\"deleted-messages-export\", \"exportTo\":\"any@james.apache.org\",\"userExportFrom\":\"james\",\"totalExportedMessages\":42, \"timestamp\":\"2018-11-13T12:00:55Z\"}";
    }

    @Override
    public TaskExecutionDetails.AdditionalInformation additionalInformation() {
        return new DeletedMessagesVaultExportTask.AdditionalInformation(USERNAME_EXPORT_FROM, EXPORT_TO, 42, TIMESTAMP);
    }

    @Override
    public AdditionalInformationDTOModule additionalInformationDTOModule() {
        return DeletedMessagesVaultExportTaskAdditionalInformationDTO.MODULE;
    }

    @Override
    public String serializedTask() {
        return "{\"type\":\"deleted-messages-export\"," +
            "\"userExportFrom\":\"james\"," +
            "\"exportQuery\":{\"combinator\":\"and\",\"criteria\":[{\"fieldName\":\"hasAttachment\",\"operator\":\"equals\",\"value\":\"true\"}]}," +
            "\"exportTo\":\"any@james.apache.org\"}";
    }

    @Override
    public Task task() {
        return new DeletedMessagesVaultExportTask(exportService, USERNAME_EXPORT_FROM, QUERY, EXPORT_TO);
    }

    @Override
    public TaskDTOModule taskDtoModule() {
        return DeletedMessagesVaultExportTaskDTO.module(factory);
    }

    @Override
    public String[] comparisonFields() {
        return new String[] {"userExportFrom", "exportTo"};
    }

    @Test
    void deleteMessagesVaultExportTaskShouldDeserializeExportQuery() throws Exception {
        Task deserializedTask = JsonTaskSerializer.of(taskDtoModule()).deserialize(serializedTask());

        DeletedMessagesVaultExportTask deserializedExportTask = (DeletedMessagesVaultExportTask) deserializedTask;
        assertThat(QUERY_TRANSLATOR.toDTO(deserializedExportTask.exportQuery)).isEqualTo(QUERY_TRANSLATOR.toDTO(QUERY));
    }

    @Test
    void additionalInformationWithInvalidMailAddressShouldThrow() {
        JsonTaskAdditionalInformationSerializer additionalInformationSerializer = JsonTaskAdditionalInformationSerializer.of(additionalInformationDTOModule());
        String invalidSerializedAdditionalInformationTask = "{\"type\":\"deleted-messages-export\",\"exportTo\":\"invalid\",\"userExportFrom\":\"james\",\"totalExportedMessages\":42}";;
        assertThatCode(() -> additionalInformationSerializer.deserialize(invalidSerializedAdditionalInformationTask))
            .hasCauseInstanceOf(AddressException.class);
    }
}