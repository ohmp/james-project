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

package org.apache.james.mailbox.cassandra.mail.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.ids.BlobId;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2;
import org.apache.james.mailbox.cassandra.mail.CassandraBlobsDAO;
import org.apache.james.mailbox.cassandra.modules.CassandraAttachmentModule;
import org.apache.james.mailbox.cassandra.modules.CassandraBlobModule;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class AttachmentV2MigrationTest {
    public static final AttachmentId ATTACHMENT_ID = AttachmentId.from("id1");
    public static final AttachmentId ATTACHMENT_ID_2 = AttachmentId.from("id2");

    @ClassRule
    public static DockerCassandraRule cassandraServer = new DockerCassandraRule();

    private CassandraCluster cassandra;
    private CassandraAttachmentDAO attachmentDAO;
    private CassandraAttachmentDAOV2 attachmentDAOV2;
    private CassandraBlobsDAO blobsDAO;
    private AttachmentV2Migration migration;
    private Attachment attachment1;
    private Attachment attachment2;

    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(
            new CassandraModuleComposite(
                new CassandraAttachmentModule(),
                new CassandraBlobModule()),
            cassandraServer.getIp(),
            cassandraServer.getBindingPort());

        attachmentDAO = new CassandraAttachmentDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        attachmentDAOV2 = new CassandraAttachmentDAOV2(cassandra.getConf());
        blobsDAO = new CassandraBlobsDAO(cassandra.getConf());

        migration = new AttachmentV2Migration(attachmentDAO, attachmentDAOV2, blobsDAO);

        attachment1 = Attachment.builder()
            .attachmentId(ATTACHMENT_ID)
            .type("application/json")
            .bytes("{\"property\":`\"value1\"}".getBytes(StandardCharsets.UTF_8))
            .build();
        attachment2 = Attachment.builder()
            .attachmentId(ATTACHMENT_ID_2)
            .type("application/json")
            .bytes("{\"property\":`\"value2\"}".getBytes(StandardCharsets.UTF_8))
            .build();
    }

    @Test
    public void emptyMigrationShouldSucceed() {
        assertThat(migration.run())
            .isEqualTo(Migration.MigrationResult.COMPLETED);
    }

    @Test
    public void migrationShouldSucceed() throws Exception {
        attachmentDAO.storeAttachment(attachment1).join();
        attachmentDAO.storeAttachment(attachment2).join();

        assertThat(migration.run())
            .isEqualTo(Migration.MigrationResult.COMPLETED);
    }

    @Test
    public void migrationShouldMoveAttachmentsToV2() throws Exception {
        attachmentDAO.storeAttachment(attachment1).join();
        attachmentDAO.storeAttachment(attachment2).join();

        migration.run();

        assertThat(attachmentDAOV2.getAttachment(ATTACHMENT_ID).join())
            .contains(CassandraAttachmentDAOV2.from(attachment1, BlobId.forPayload(attachment1.getBytes())));
        assertThat(attachmentDAOV2.getAttachment(ATTACHMENT_ID_2).join())
            .contains(CassandraAttachmentDAOV2.from(attachment2, BlobId.forPayload(attachment2.getBytes())));
    }

    @Test
    public void migrationShouldRemoveAttachmentsFromV() throws Exception {
        attachmentDAO.storeAttachment(attachment1).join();
        attachmentDAO.storeAttachment(attachment2).join();

        migration.run();

        assertThat(attachmentDAO.getAttachment(ATTACHMENT_ID).join())
            .isEmpty();
        assertThat(attachmentDAO.getAttachment(ATTACHMENT_ID_2).join())
            .isEmpty();
    }
}