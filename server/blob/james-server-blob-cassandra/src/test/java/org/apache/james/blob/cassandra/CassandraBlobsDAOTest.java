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

package org.apache.james.blob.cassandra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraExtension;
import org.apache.james.backends.cassandra.init.CassandraConfiguration;
import org.apache.james.blob.api.BlobId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Strings;

@ExtendWith(DockerCassandraExtension.class)
public class CassandraBlobsDAOTest {
    private static final int CHUNK_SIZE = 1024;
    private static final int MULTIPLE_CHUNK_SIZE = 3;

    private CassandraCluster cassandra;
    private CassandraBlobsDAO testee;
    private static final BlobId BLOB_ID = new CassandraBlobId("any");

    @BeforeEach
    public void setUp(DockerCassandraExtension.DockerCassandra dockerCassandra) {
        cassandra = CassandraCluster.create(
            new CassandraBlobModule(), dockerCassandra.getIp(), dockerCassandra.getBindingPort());

        testee = new CassandraBlobsDAO(cassandra.getConf(),
            CassandraConfiguration.builder()
                .blobPartSize(CHUNK_SIZE)
                .build());
    }

    @AfterEach
    public void tearDown() {
        cassandra.close();
    }

    @Test
    public void saveShouldReturnEmptyWhenNullData() throws Exception {
        assertThatThrownBy(() -> testee.save(BLOB_ID, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void saveShouldReturnEmptyWhenNullBlobId() throws Exception {
        assertThatThrownBy(() -> testee.save(null, "".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void saveShouldSaveEmptyData() throws Exception {
        testee.save(BLOB_ID, new byte[]{}).join();

        byte[] bytes = testee.read(BLOB_ID).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    public void saveShouldSaveBlankData() throws Exception {
        BlobId blobId = testee.save(BLOB_ID, "".getBytes(StandardCharsets.UTF_8)).join();

        byte[] bytes = testee.read(blobId).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    public void readShouldBeEmptyWhenNoExisting() throws IOException {
        byte[] bytes = testee.read(new CassandraBlobId("unknown")).join();

        assertThat(bytes).isEmpty();
    }

    @Test
    public void readShouldReturnSavedData() throws IOException {
        testee.save(BLOB_ID, "toto".getBytes(StandardCharsets.UTF_8)).join();

        byte[] bytes = testee.read(BLOB_ID).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("toto");
    }

    @Test
    public void readShouldReturnLongSavedData() throws IOException {
        String longString = Strings.repeat("0123456789\n", 1000);
        testee.save(BLOB_ID, longString.getBytes(StandardCharsets.UTF_8)).join();

        byte[] bytes = testee.read(BLOB_ID).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(longString);
    }

    @Test
    public void readShouldReturnSplitSavedDataByChunk() throws IOException {
        String longString = Strings.repeat("0123456789\n", MULTIPLE_CHUNK_SIZE);
        testee.save(BLOB_ID, longString.getBytes(StandardCharsets.UTF_8)).join();

        byte[] bytes = testee.read(BLOB_ID).join();

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(longString);
    }
}