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

import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.apache.james.core.Username;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.server.task.json.TaskAdditionalInformationSerializationContract;
import org.apache.james.server.task.json.TaskSerializationContract;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.vault.DeletedMessageVault;
import org.junit.jupiter.api.BeforeEach;

class DeletedMessagesVaultDeleteTaskSerializationTest implements TaskSerializationContract, TaskAdditionalInformationSerializationContract {
    static final Instant TIMESTAMP = Instant.parse("2018-11-13T12:00:55Z");
    static final Username USERNAME = Username.of("james");
    static final TestMessageId.Factory MESSAGE_ID_FACTORY = new TestMessageId.Factory();
    static final MessageId MESSAGE_ID = MESSAGE_ID_FACTORY.generate();

    DeletedMessageVault deletedMessageVault;
    DeletedMessagesVaultDeleteTask.Factory factory;

    @BeforeEach
    void setUp() {
        deletedMessageVault = mock(DeletedMessageVault.class);
        factory = new DeletedMessagesVaultDeleteTask.Factory(deletedMessageVault, MESSAGE_ID_FACTORY);
    }

    @Override
    public String serializedAdditionalInformation() {
        return "{\"type\": \"deleted-messages-delete\", \"userName\":\"james\", \"messageId\": \"" + MESSAGE_ID.serialize() + "\", \"timestamp\":\"2018-11-13T12:00:55Z\"}";
    }

    @Override
    public TaskExecutionDetails.AdditionalInformation additionalInformation() {
        return new DeletedMessagesVaultDeleteTask.AdditionalInformation(USERNAME, MESSAGE_ID, TIMESTAMP);
    }

    @Override
    public AdditionalInformationDTOModule additionalInformationDTOModule() {
        return DeletedMessagesVaultDeleteTaskAdditionalInformationDTO.serializationModule(MESSAGE_ID_FACTORY);
    }

    @Override
    public String serializedTask() {
        return "{\"type\": \"deleted-messages-delete\", \"userName\":\"james\", \"messageId\": \"" + MESSAGE_ID.serialize() + "\"}";
    }

    @Override
    public Task task() {
        return new DeletedMessagesVaultDeleteTask(deletedMessageVault, USERNAME, MESSAGE_ID);
    }

    @Override
    public TaskDTOModule taskDtoModule() {
        return DeletedMessagesVaultDeleteTaskDTO.module(factory);
    }

    @Override
    public String[] comparisonFields() {
        return new String[] {"username", "messageId"};
    }
}