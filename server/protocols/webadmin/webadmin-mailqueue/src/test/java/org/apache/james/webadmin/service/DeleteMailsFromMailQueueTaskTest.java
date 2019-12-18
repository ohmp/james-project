/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.webadmin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.internet.AddressException;

import org.apache.james.JsonSerializationVerifier;
import org.apache.james.core.MailAddress;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.server.task.json.JsonTaskAdditionalInformationSerializer;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeleteMailsFromMailQueueTaskTest {
    private static final Instant TIMESTAMP = Instant.parse("2018-11-13T12:00:55Z");

    private MailQueueFactory<ManageableMailQueue> mailQueueFactory;
    private ManageableMailQueue mockedQueue;
    private final static String queueName = "anyQueue";

    @BeforeEach
    void setUp() {
        mailQueueFactory = mock(MailQueueFactory.class);
        mockedQueue = mock(ManageableMailQueue.class);
        when(mockedQueue.getName()).thenReturn(queueName);
        when(mailQueueFactory.getQueue(anyString())).thenAnswer(arg -> Optional.of(mockedQueue));
    }

    @ParameterizedTest
    @MethodSource
    void taskShouldBeSerializable(Optional<MailAddress> sender, Optional<String> name, Optional<MailAddress> recipient, String serializedJson) throws Exception {
        ManageableMailQueue queue = mailQueueFactory.getQueue(queueName).get();
        DeleteMailsFromMailQueueTask task = new DeleteMailsFromMailQueueTask(queue, sender, name, recipient);

        JsonSerializationVerifier.dtoModule(DeleteMailsFromMailQueueTaskDTO.module(mailQueueFactory))
            .bean(task)
            .json(serializedJson)
            .verify();
    }

    private static Stream<Arguments> taskShouldBeSerializable() throws Exception {
        return Stream.of(
            Arguments.of(Optional.of(new MailAddress("a@b.c")), Optional.empty(), Optional.empty(), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"sender\": \"a@b.c\"}"),
            Arguments.of(Optional.empty(), Optional.of("name"), Optional.empty(), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"name\": \"name\"}"),
            Arguments.of(Optional.empty(), Optional.empty(), Optional.of(new MailAddress("d@e.f")), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"recipient\": \"d@e.f\"}"));
    }

    @Test
    void taskShouldThrowWhenDeserializeAnUnknownQueue() {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = mock(MailQueueFactory.class);
        when(mailQueueFactory.getQueue(anyString())).thenReturn(Optional.empty());
        JsonTaskSerializer testee = JsonTaskSerializer.of(DeleteMailsFromMailQueueTaskDTO.module(mailQueueFactory));

        String serializedJson = "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"sender\": \"a@b.c\"}";
        assertThatThrownBy(() -> testee.deserialize(serializedJson))
            .isInstanceOf(DeleteMailsFromMailQueueTask.UnknownSerializedQueue.class);
    }

    @Test
    void taskShouldThrowWhenDeserializeAMalformedMailAddress() {
        JsonTaskSerializer testee = JsonTaskSerializer.of(DeleteMailsFromMailQueueTaskDTO.module(mailQueueFactory));

        String serializedJson = "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"" + queueName + "\", \"sender\": \"a.b.c\"}";
        assertThatThrownBy(() -> testee.deserialize(serializedJson))
            .isInstanceOf(AddressException.class);
    }

    private static Stream<Arguments> additionalInformationShouldBeSerializable() throws Exception {
        return Stream.of(
            Arguments.of(10L, 5L, Optional.of(new MailAddress("a@b.c")), Optional.empty(), Optional.empty(), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"sender\": \"a@b.c\", \"initialCount\" : 10, \"remainingCount\":5, \"timestamp\": \"2018-11-13T12:00:55Z\"}"),
            Arguments.of(1L, 0L, Optional.empty(), Optional.of("name"), Optional.empty(), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"name\": \"name\", \"initialCount\" : 1, \"remainingCount\":0, \"timestamp\": \"2018-11-13T12:00:55Z\"}"),
            Arguments.of(6L, 6L, Optional.empty(), Optional.empty(), Optional.of(new MailAddress("d@e.f")), "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"recipient\": \"d@e.f\", \"initialCount\" : 6, \"remainingCount\":6, \"timestamp\": \"2018-11-13T12:00:55Z\"}"));
    }

    @ParameterizedTest
    @MethodSource
    void additionalInformationShouldBeSerializable(long initialCount,
                                                   long remainingCount,
                                                   Optional<MailAddress> sender,
                                                   Optional<String> name,
                                                   Optional<MailAddress> recipient,
                                                   String serializedAdditionalInformationJson) throws Exception {

        ManageableMailQueue queue = mailQueueFactory.getQueue(queueName).get();
        DeleteMailsFromMailQueueTask.AdditionalInformation details = new DeleteMailsFromMailQueueTask.AdditionalInformation(queue.getName(), initialCount, remainingCount, sender, name, recipient, TIMESTAMP);

        JsonSerializationVerifier.dtoModule(DeleteMailsFromMailQueueTaskAdditionalInformationDTO.MODULE)
            .bean(details)
            .json(serializedAdditionalInformationJson)
            .verify();
    }

    @Test
    void additionalInformationShouldThrowWhenDeserializeAMalformedMailAddress() {
        String serializedJson = "{\"type\": \"delete-mails-from-mail-queue\", \"queue\": \"anyQueue\", \"sender\": \"a.b.c\", \"initialCount\" : 10, \"remainingCount\":5}";
        assertThatThrownBy(() -> JsonTaskAdditionalInformationSerializer.of(DeleteMailsFromMailQueueTaskAdditionalInformationDTO.MODULE)
                .deserialize(serializedJson))
            .isInstanceOf(AddressException.class);
    }

}
