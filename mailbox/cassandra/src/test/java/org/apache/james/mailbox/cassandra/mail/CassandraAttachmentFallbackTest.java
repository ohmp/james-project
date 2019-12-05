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

package org.apache.james.mailbox.cassandra.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.CassandraRestartExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobStore;
import org.apache.james.mailbox.cassandra.ids.CassandraMessageId;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO.DAOAttachmentV1;
import org.apache.james.mailbox.cassandra.modules.CassandraAttachmentModule;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(CassandraRestartExtension.class)
class CassandraAttachmentFallbackTest {
    private static final AttachmentId ATTACHMENT_ID_1 = AttachmentId.from("id1");
    private static final AttachmentId ATTACHMENT_ID_2 = AttachmentId.from("id2");
    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    private static final byte[] BYTES = "{\"property\":`\"value\"}".getBytes(StandardCharsets.UTF_8);
    private static final Attachment ATTACHMENT = Attachment.builder()
        .attachmentId(ATTACHMENT_ID_1)
        .type("application/json")
        .size(BYTES.length)
        .build();
    private static final DAOAttachmentV1 DAO_ATTACHMENT = new DAOAttachmentV1(BYTES, ATTACHMENT);
    private static final byte[] BYTES_2 = "{\"property\":`\"different\"}".getBytes(StandardCharsets.UTF_8);
    private static final Attachment OTHER_ATTACHMENT = Attachment.builder()
        .attachmentId(ATTACHMENT_ID_2)
        .type("application/json")
        .size(BYTES_2.length)
        .build();
    private static final DAOAttachmentV1 OTHER_DAO_ATTACHMENT = new DAOAttachmentV1(BYTES_2, OTHER_ATTACHMENT);

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(
            CassandraAttachmentModule.MODULE,
            CassandraBlobModule.MODULE));

    private CassandraAttachmentDAOV2 attachmentDAOV2;
    private CassandraAttachmentDAO attachmentDAO;
    private CassandraAttachmentMapper attachmentMapper;
    private CassandraBlobStore blobStore;
    private CassandraAttachmentMessageIdDAO attachmentMessageIdDAO;


    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        attachmentDAOV2 = new CassandraAttachmentDAOV2(BLOB_ID_FACTORY, cassandra.getConf());
        attachmentDAO = new CassandraAttachmentDAO(cassandra.getConf(),
            CassandraConfiguration.DEFAULT_CONFIGURATION);
        blobStore = new CassandraBlobStore(cassandra.getConf());
        attachmentMessageIdDAO = new CassandraAttachmentMessageIdDAO(cassandra.getConf(), new CassandraMessageId.Factory());
        CassandraAttachmentOwnerDAO ownerDAO = new CassandraAttachmentOwnerDAO(cassandra.getConf());
        attachmentMapper = new CassandraAttachmentMapper(attachmentDAO, attachmentDAOV2, blobStore, attachmentMessageIdDAO, ownerDAO);
    }

    @Test
    void getAttachmentShouldThrowWhenAbsentFromV1AndV2() {
        assertThatThrownBy(() -> attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void getAttachmentsShouldReturnEmptyWhenAbsentFromV1AndV2() {
        assertThat(attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .isEmpty();
    }

    @Test
    void getAttachmentShouldReturnV2WhenPresentInV1AndV2() throws Exception {
        BlobId blobId = blobStore.save(blobStore.getDefaultBucketName(), BYTES).block();
        attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(ATTACHMENT, blobId)).block();
        attachmentDAO.storeAttachment(OTHER_DAO_ATTACHMENT).block();

        assertThat(attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isEqualTo(ATTACHMENT);
    }

    @Test
    void getAttachmentShouldReturnV1WhenV2Absent() throws Exception {
        attachmentDAO.storeAttachment(DAO_ATTACHMENT).block();

        assertThat(attachmentMapper.getAttachment(ATTACHMENT_ID_1))
            .isEqualTo(ATTACHMENT);
    }

    @Test
    void getAttachmentsShouldReturnV2WhenV2AndV1() throws Exception {
        BlobId blobId = blobStore.save(blobStore.getDefaultBucketName(), BYTES).block();
        attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(ATTACHMENT, blobId)).block();
        attachmentDAO.storeAttachment(OTHER_DAO_ATTACHMENT).block();

        assertThat(attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .containsExactly(ATTACHMENT);
    }

    @Test
    void getAttachmentsShouldReturnV1WhenV2Absent() throws Exception {
        attachmentDAO.storeAttachment(DAO_ATTACHMENT).block();

        assertThat(attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1)))
            .containsExactly(ATTACHMENT);
    }

    @Test
    void getAttachmentsShouldCombineElementsFromV1AndV2() throws Exception {
        BlobId blobId = blobStore.save(blobStore.getDefaultBucketName(), BYTES).block();
        attachmentDAOV2.storeAttachment(CassandraAttachmentDAOV2.from(ATTACHMENT, blobId)).block();
        attachmentDAO.storeAttachment(OTHER_DAO_ATTACHMENT).block();

        List<Attachment> attachments = attachmentMapper.getAttachments(ImmutableList.of(ATTACHMENT_ID_1, ATTACHMENT_ID_2));
        assertThat(attachments)
            .containsExactlyInAnyOrder(ATTACHMENT, OTHER_ATTACHMENT);
    }
}
