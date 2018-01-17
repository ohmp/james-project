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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.ObjectStore;

import com.google.common.base.Preconditions;

public class CassandraObjectStore implements ObjectStore {
    private final CassandraBlobsDAO blobsDAO;

    @Inject
    public CassandraObjectStore(CassandraBlobsDAO blobsDAO) {
        this.blobsDAO = blobsDAO;
    }

    @Override
    public OutputStream save(BlobId blobId) {
        Preconditions.checkNotNull(blobId);
        return new CassandraBlobOutputStream(blobId);
    }

    @Override
    public InputStream read(BlobId blobId) {
        Preconditions.checkNotNull(blobId);
        return new ByteArrayInputStream(blobsDAO.read(blobId).join());
    }

    @Override
    public CompletableFuture<BlobId> saveAsBytes(BlobId blobId, byte[] data) {
        Preconditions.checkNotNull(blobId);
        Preconditions.checkNotNull(data);
        return blobsDAO.save(blobId, data);
    }

    @Override
    public CompletableFuture<byte[]> readAsBytes(BlobId blobId) {
        Preconditions.checkNotNull(blobId);
        return blobsDAO.read(blobId);
    }

    public class CassandraBlobOutputStream extends ByteArrayOutputStream {
        private final BlobId blobId;

        public CassandraBlobOutputStream(BlobId blobId) {
            this.blobId = blobId;
        }

        @Override
        public void close() throws IOException {
            saveAsBytes(blobId, toByteArray()).join();
            super.close();
        }
    }
}
