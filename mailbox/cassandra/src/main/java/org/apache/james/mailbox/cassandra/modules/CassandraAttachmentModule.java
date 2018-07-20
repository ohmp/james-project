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

package org.apache.james.mailbox.cassandra.modules;

import static com.datastax.driver.core.DataType.bigint;
import static com.datastax.driver.core.DataType.blob;
import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.DataType.uuid;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.mailbox.cassandra.table.CassandraAttachmentMessageIdTable;
import org.apache.james.mailbox.cassandra.table.CassandraAttachmentOwnerTable;
import org.apache.james.mailbox.cassandra.table.CassandraAttachmentTable;
import org.apache.james.mailbox.cassandra.table.CassandraAttachmentV2Table;

import com.datastax.driver.core.schemabuilder.SchemaBuilder;

public interface CassandraAttachmentModule {

    CassandraModule CASSANDRA_ATTACHMENT_TABLE = CassandraModule.table(CassandraAttachmentTable.TABLE_NAME)
        .statement(SchemaBuilder.createTable(CassandraAttachmentTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraAttachmentTable.ID, text())
            .addColumn(CassandraAttachmentTable.PAYLOAD, blob())
            .addColumn(CassandraAttachmentTable.TYPE, text())
            .addColumn(CassandraAttachmentTable.SIZE, bigint())
            .withOptions()
            .comment("Holds attachment for fast attachment retrieval"))
        .build();

    CassandraModule CASSANDRA_ATTACHMENT_V2_TABLE = CassandraModule.table(CassandraAttachmentV2Table.TABLE_NAME)
        .statement(SchemaBuilder.createTable(CassandraAttachmentV2Table.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraAttachmentV2Table.ID_AS_UUID, uuid())
            .addColumn(CassandraAttachmentV2Table.ID, text())
            .addColumn(CassandraAttachmentV2Table.BLOB_ID, text())
            .addColumn(CassandraAttachmentV2Table.TYPE, text())
            .addColumn(CassandraAttachmentV2Table.SIZE, bigint())
            .withOptions()
            .compactionOptions(SchemaBuilder.leveledStrategy())
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION))
            .comment("Holds attachment for fast attachment retrieval. Content of messages is stored" +
                "in `blobs` and `blobparts` tables."))
        .build();

    CassandraModule CASSANDRA_ATTACHMENT_MESSAGE_ID_TABLE = CassandraModule.table(CassandraAttachmentMessageIdTable.TABLE_NAME)
        .statement(SchemaBuilder.createTable(CassandraAttachmentMessageIdTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraAttachmentMessageIdTable.ATTACHMENT_ID_AS_UUID, uuid())
            .addColumn(CassandraAttachmentMessageIdTable.ATTACHMENT_ID, text())
            .addClusteringColumn(CassandraAttachmentMessageIdTable.MESSAGE_ID, text())
            .withOptions()
            .compactionOptions(SchemaBuilder.leveledStrategy())
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION))
            .comment("Holds ids of messages owning the attachment"))
        .build();

    CassandraModule CASSANDRA_ATTACHMENT_OWNER_TABLE = CassandraModule.table(CassandraAttachmentOwnerTable.TABLE_NAME)
        .statement(SchemaBuilder.createTable(CassandraAttachmentOwnerTable.TABLE_NAME)
            .ifNotExists()
            .addPartitionKey(CassandraAttachmentOwnerTable.ID, uuid())
            .addClusteringColumn(CassandraAttachmentOwnerTable.OWNER, text())
            .withOptions()
            .compactionOptions(SchemaBuilder.leveledStrategy())
            .caching(SchemaBuilder.KeyCaching.ALL,
                SchemaBuilder.rows(CassandraConstants.DEFAULT_CACHED_ROW_PER_PARTITION))
            .comment("Holds explicit owners of some attachments"))
        .build();

    CassandraModule MODULE = new CassandraModuleComposite(
        CASSANDRA_ATTACHMENT_TABLE,
        CASSANDRA_ATTACHMENT_V2_TABLE,
        CASSANDRA_ATTACHMENT_MESSAGE_ID_TABLE,
        CASSANDRA_ATTACHMENT_OWNER_TABLE);
}
