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

package org.apache.james.mailrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public interface MailRepositoryContract {

    String TEST_ATTRIBUTE = "testAttribute";

    default  Mail createMail(String name) throws MessagingException {
        return createMail(name, "original body");
    }

    default  Mail createMail(String name, String body) throws MessagingException {
        MimeMessage mailContent = generateMailContent(body);
        List<MailAddress> recipients = ImmutableList
            .of(new MailAddress("rec1@domain.com"),
                new MailAddress("rec2@domain.com"));
        MailAddress sender = new MailAddress("sender@domain.com");
        Mail mail = new MailImpl(name, sender, recipients, mailContent);
        mail.setAttribute(TEST_ATTRIBUTE, "testValue");
        return mail;
    }


    default MimeMessage generateMailContent(String body) throws MessagingException {
        return MimeMessageBuilder.mimeMessageBuilder()
            .setSubject("test")
            .setText(body)
            .build();
    }

    default void checkMailEquality(Mail actual, Mail expected) {
        assertAll(
            () -> assertThat(actual.getMessage().getContent()).isEqualTo(expected.getMessage().getContent()),
            () -> assertThat(actual.getMessageSize()).isEqualTo(expected.getMessageSize()),
            () -> assertThat(actual.getName()).isEqualTo(expected.getName()),
            () -> assertThat(actual.getState()).isEqualTo(expected.getState()),
            () -> assertThat(actual.getAttribute(TEST_ATTRIBUTE)).isEqualTo(expected.getAttribute(TEST_ATTRIBUTE))
        );
    }

    MailRepository retrieveRepository() throws Exception;

    @Test
    default void storeRegularMailShouldNotFail() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail("mail1");

        testee.store(mail);
    }

    @Test
    default void retrieveShouldGetStoredMail() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        Mail mail = createMail(key1);

        testee.store(mail);

        assertThat(testee.retrieve(key1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @Test
    default void newlyCreatedRepositoryShouldNotContainAnyMail() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.list()).isEmpty();
    }

    @Test
    default void retrievingUnknownMailShouldReturnNull() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.retrieve("random")).isNull();
    }

    @Test
    default void removingUnknownMailShouldHaveNoEffect() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove("random");
    }

    @Test
    default void listShouldReturnStoredMailsKeys() throws Exception {
        MailRepository testee = retrieveRepository();
        String key1 = "mail1";
        String key2 = "mail2";
        testee.store(createMail(key1));

        testee.store(createMail(key2));

        assertThat(testee.list()).containsExactly(key1, key2);
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageContent() throws Exception {
        MailRepository testee = retrieveRepository();
        String key = "mail1";
        testee.store(createMail(key));

        Mail updatedMail = createMail(key, "modified content");
        testee.store(updatedMail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(key)).satisfies(actual -> checkMailEquality(actual, updatedMail));
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageAttributes() throws Exception {
        MailRepository testee = retrieveRepository();
        String key = "mail1";
        Mail mail = createMail(key);
        testee.store(mail);

        mail.setAttribute(TEST_ATTRIBUTE, "newValue");
        testee.store(mail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(key)).satisfies(actual -> checkMailEquality(actual, mail));
    }

}
