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
package org.apache.james.webadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailrepository.api.MailKey;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.mailrepository.memory.MemoryMailRepository;
import org.apache.james.server.core.MimeMessageInputStream;
import org.apache.james.util.ClassLoaderUtils;
import org.apache.james.util.streams.Limit;
import org.apache.james.util.streams.Offset;
import org.apache.james.webadmin.dto.MailKeyDTO;
import org.apache.james.webadmin.dto.MailRepositoryResponse;
import org.apache.mailet.base.test.FakeMail;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class MailRepositoryStoreServiceTest {
    private static final MailRepositoryUrl FIRST_REPOSITORY = new MailRepositoryUrl("url://repository");
    private static final MailRepositoryUrl SECOND_REPOSITORY = new MailRepositoryUrl("url://repository2");
    private static final MailKey NAME_1 = new MailKey("name1");
    private static final MailKey NAME_2 = new MailKey("name2");

    private MailRepositoryStore mailRepositoryStore;
    private MailRepositoryStoreService testee;
    private MemoryMailRepository repository;

    @Before
    public void setUp() {
        mailRepositoryStore = mock(MailRepositoryStore.class);
        repository = new MemoryMailRepository();
        testee = new MailRepositoryStoreService(mailRepositoryStore);
    }

    @Test
    public void listMailRepositoriesShouldReturnEmptyWhenEmpty() {
        assertThat(testee.listMailRepositories()).isEmpty();
    }

    @Test
    public void listMailRepositoriesShouldReturnOneRepositoryWhenOne() {
        when(mailRepositoryStore.getUrls())
            .thenReturn(ImmutableList.of(FIRST_REPOSITORY));
        assertThat(testee.listMailRepositories())
            .extracting(MailRepositoryResponse::getRepository)
            .containsOnly(FIRST_REPOSITORY.asString());
    }

    @Test
    public void listMailRepositoriesShouldReturnTwoRepositoriesWhentwo() {
        when(mailRepositoryStore.getUrls())
            .thenReturn(ImmutableList.of(FIRST_REPOSITORY, SECOND_REPOSITORY));
        assertThat(testee.listMailRepositories())
            .extracting(MailRepositoryResponse::getRepository)
            .containsOnly(FIRST_REPOSITORY.asString(), SECOND_REPOSITORY.asString());
    }

    @Test
    public void listMailsShouldThrowWhenMailRepositoryStoreThrows() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY))
            .thenThrow(new MailRepositoryStore.MailRepositoryStoreException("message"));

        assertThatThrownBy(() -> testee.listMails(FIRST_REPOSITORY, Offset.none(), Limit.unlimited()))
            .isInstanceOf(MailRepositoryStore.MailRepositoryStoreException.class);
    }

    @Test
    public void listMailsShouldReturnEmptyWhenMailRepositoryIsEmpty() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY)).thenReturn(Optional.of(repository));

        assertThat(testee.listMails(FIRST_REPOSITORY, Offset.none(), Limit.unlimited()).get())
            .isEmpty();
    }

    @Test
    public void listMailsShouldReturnContainedMailKeys() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY)).thenReturn(Optional.of(repository));

        repository.store(FakeMail.builder()
            .name(NAME_1.asString())
            .build());
        repository.store(FakeMail.builder()
            .name(NAME_2.asString())
            .build());

        assertThat(testee.listMails(FIRST_REPOSITORY, Offset.none(), Limit.unlimited()).get())
            .containsOnly(new MailKeyDTO(NAME_1), new MailKeyDTO(NAME_2));
    }

    @Test
    public void listMailsShouldApplyLimitAndOffset() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY)).thenReturn(Optional.of(repository));

        repository.store(FakeMail.builder()
            .name(NAME_1.asString())
            .build());
        repository.store(FakeMail.builder()
            .name(NAME_2.asString())
            .build());
        repository.store(FakeMail.builder()
            .name("name3")
            .build());

        assertThat(testee.listMails(FIRST_REPOSITORY, Offset.from(1), Limit.from(1)).get())
            .containsOnly(new MailKeyDTO(NAME_2));
    }

    @Test
    public void retrieveMessageShouldThrownWhenUnknownRepository() throws Exception {
        when(mailRepositoryStore.get(new MailRepositoryUrl("unkown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testee.retrieveMessage(FIRST_REPOSITORY, NAME_1))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void retrieveMessageShouldThrowWhenMailRepositoryStoreThrows() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY))
            .thenThrow(new MailRepositoryStore.MailRepositoryStoreException("message"));

        assertThatThrownBy(() -> testee.retrieveMessage(FIRST_REPOSITORY, NAME_1))
            .isInstanceOf(MailRepositoryStore.MailRepositoryStoreException.class);
    }

    @Test
    public void retrieveMessageShouldReturnEmptyWhenMailNotFound() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY)).thenReturn(Optional.of(repository));

        assertThat(testee.retrieveMessage(FIRST_REPOSITORY, NAME_1))
            .isEmpty();
    }

    @Test
    public void retrieveMessageShouldReturnTheMessageWhenMailExists() throws Exception {
        when(mailRepositoryStore.get(FIRST_REPOSITORY)).thenReturn(Optional.of(repository));

        FakeMail mail = FakeMail.builder()
            .name(NAME_1.asString())
            .fileName("mail.eml")
            .build();
        repository.store(mail);

        Optional<MimeMessage> mimeMessage = testee.retrieveMessage(FIRST_REPOSITORY, NAME_1);
        assertThat(mimeMessage).isNotEmpty();

        String eml = IOUtils.toString(new MimeMessageInputStream(mimeMessage.get()), StandardCharsets.UTF_8);
        String expectedContent = ClassLoaderUtils.getSystemResourceAsString("mail.eml");
        assertThat(eml).isEqualToNormalizingNewlines(expectedContent);
    }
}
