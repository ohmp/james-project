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

package org.apache.james.mailrepository.cassandra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.TestBlobId;
import org.apache.mailet.PerRecipientHeaders;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

@ExtendWith(DockerCassandraExtension.class)
public class CassandraMailRepositoryMailDAOTest {

    static final String URL = "url";
    static final String KEY_1 = "key1";
    static final TestBlobId.Factory BLOB_ID_FACTORY = new TestBlobId.Factory();

    CassandraCluster cassandra;
    CassandraMailRepositoryMailDAO testee;

    @BeforeEach
    public void setUp(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(
            new CassandraMailRepositoryModule(), dockerCassandra.getIp(), dockerCassandra.getBindingPort());

        testee = new CassandraMailRepositoryMailDAO(cassandra.getConf(), BLOB_ID_FACTORY, cassandra.getTypesProvider());
    }

    @AfterEach
    public void tearDown() {
        cassandra.close();
    }

    @Test
    public void readShouldReturnEmptyWhenAbsent() {
        assertThat(testee.read(URL, KEY_1).join())
            .isEmpty();
    }

    @Test
    public void readShouldReturnAllMailMetadata() throws Exception {
        BlobId blobIdBody = BLOB_ID_FACTORY.from("blobHeader");
        BlobId blobIdHeader = BLOB_ID_FACTORY.from("blobBody");
        String errorMessage = "error message";
        String state = "state";
        String remoteAddr = "remoteAddr";
        String remoteHost = "remoteHost";
        PerRecipientHeaders.Header header = PerRecipientHeaders.Header.builder().name("headerName").value("headerValue").build();
        String attributeName = "att1";
        ImmutableList<String> attributeValue = ImmutableList.of("value1", "value2");

        testee.store(URL,
            FakeMail.builder()
                .name(KEY_1)
                .sender(MailAddressFixture.SENDER)
                .recipients(MailAddressFixture.RECIPIENT1, MailAddressFixture.RECIPIENT2)
                .errorMessage(errorMessage)
                .state(state)
                .remoteAddr(remoteAddr)
                .remoteHost(remoteHost)
                .addHeaderForRecipient(header, MailAddressFixture.RECIPIENT1)
                .attribute(attributeName, attributeValue)
                .build(),
            blobIdHeader,
            blobIdBody)
            .join();

        CassandraMailRepositoryMailDAO.MailDTO mailDTO = testee.read(URL, KEY_1).join().get();

        assertAll(
            () -> assertThat(mailDTO.getBodyBlobId()).isEqualTo(blobIdBody),
            () -> assertThat(mailDTO.getHeaderBlobId()).isEqualTo(blobIdHeader),
            () -> assertThat(mailDTO.getMail().getName()).isEqualTo(KEY_1),
            () -> assertThat(mailDTO.getMail().getErrorMessage()).isEqualTo(errorMessage),
            () -> assertThat(mailDTO.getMail().getState()).isEqualTo(state),
            () -> assertThat(mailDTO.getMail().getRemoteAddr()).isEqualTo(remoteAddr),
            () -> assertThat(mailDTO.getMail().getRemoteHost()).isEqualTo(remoteHost),
            () -> assertThat(mailDTO.getMail().getAttributeNames()).containsOnly(attributeName),
            () -> assertThat(mailDTO.getMail().getAttribute(attributeName)).isEqualTo(attributeValue),
            () -> assertThat(mailDTO.getMail().getPerRecipientSpecificHeaders().getRecipientsWithSpecificHeaders())
                .containsOnly(MailAddressFixture.RECIPIENT1),
            () -> assertThat(mailDTO.getMail().getPerRecipientSpecificHeaders().getHeadersForRecipient(MailAddressFixture.RECIPIENT1))
                .containsOnly(header),
            () -> assertThat(mailDTO.getMail().getSender()).isEqualTo(MailAddressFixture.SENDER),
            () -> assertThat(mailDTO.getMail().getRecipients()).containsOnly(MailAddressFixture.RECIPIENT1, MailAddressFixture.RECIPIENT2));
    }

    @Test
    public void storeShouldAcceptMailWithOnlyName() throws Exception {
        BlobId blobIdBody = BLOB_ID_FACTORY.from("blobHeader");
        BlobId blobIdHeader = BLOB_ID_FACTORY.from("blobBody");

        testee.store(URL,
            FakeMail.builder()
                .name(KEY_1)
                .build(),
            blobIdHeader,
            blobIdBody)
            .join();

        CassandraMailRepositoryMailDAO.MailDTO mailDTO = testee.read(URL, KEY_1).join().get();

        assertAll(
            () -> assertThat(mailDTO.getBodyBlobId()).isEqualTo(blobIdBody),
            () -> assertThat(mailDTO.getHeaderBlobId()).isEqualTo(blobIdHeader),
            () -> assertThat(mailDTO.getMail().getName()).isEqualTo(KEY_1));
    }

    @Test
    public void removeShouldDeleteMailMetaData() throws Exception {
        BlobId blobIdBody = BLOB_ID_FACTORY.from("blobHeader");
        BlobId blobIdHeader = BLOB_ID_FACTORY.from("blobBody");

        testee.store(URL,
            FakeMail.builder()
                .name(KEY_1)
                .build(),
            blobIdHeader,
            blobIdBody)
            .join();

        testee.remove(URL, KEY_1);

        assertThat(testee.read(URL, KEY_1).join())
            .isEmpty();
    }

}