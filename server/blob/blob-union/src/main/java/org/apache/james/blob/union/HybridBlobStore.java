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

package org.apache.james.blob.union;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import reactor.core.publisher.Mono;

public class HybridBlobStore implements BlobStore {
    @FunctionalInterface
    public interface RequireLowCost {
        RequirePerforming lowCost(BlobStore blobStore);
    }

    @FunctionalInterface
    public interface RequirePerforming {
        Builder performing(BlobStore blobStore);
    }

    public static class Builder {
        private final BlobStore lowCostBlobStore;
        private final BlobStore legacyBlobStore;

        Builder(BlobStore lowCostBlobStore, BlobStore legacyBlobStore) {
            this.lowCostBlobStore = lowCostBlobStore;
            this.legacyBlobStore = legacyBlobStore;
        }

        public HybridBlobStore build() {
            return new HybridBlobStore(
                lowCostBlobStore,
                legacyBlobStore);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(HybridBlobStore.class);
    private static final int SIZE_THRESHOLD = 32 * 1024;

    public static RequireLowCost builder() {
        return lowCost -> performing -> new Builder(lowCost, performing);
    }

    private final BlobStore lowCostBlobStore;
    private final BlobStore performingBlobStore;

    private HybridBlobStore(BlobStore lowCostBlobStore, BlobStore performingBlobStore) {
        this.lowCostBlobStore = lowCostBlobStore;
        this.performingBlobStore = performingBlobStore;
    }

    @Override
    public Mono<BlobId> save(BucketName bucketName, byte[] data, StoragePolicy storagePolicy) {
        return selectBlobStore(storagePolicy, () -> data.length > SIZE_THRESHOLD)
            .save(bucketName, data, storagePolicy);
    }

    @Override
    public Mono<BlobId> save(BucketName bucketName, InputStream data, StoragePolicy storagePolicy) {
        Preconditions.checkNotNull(data);

        BufferedInputStream bufferedInputStream = new BufferedInputStream(data, SIZE_THRESHOLD + 1);
        return selectBlobStore(storagePolicy, Throwing.supplier(() -> isItABigStream(bufferedInputStream)))
            .save(bucketName, bufferedInputStream, storagePolicy);
    }

    private BlobStore selectBlobStore(StoragePolicy storagePolicy, Supplier<Boolean> largeData) {
        switch (storagePolicy) {
            case LowCost:
                return lowCostBlobStore;
            case SizeBased:
                if (largeData.get()) {
                    return lowCostBlobStore;
                }
                return performingBlobStore;
            case Performing:
                return performingBlobStore;
            default:
                throw new RuntimeException("Unknown storage policy: " + storagePolicy);
        }
    }

    private boolean isItABigStream(InputStream bufferedData) throws IOException {
        bufferedData.mark(0);
        bufferedData.skip(SIZE_THRESHOLD);
        boolean isItABigStream = bufferedData.read() != -1;
        bufferedData.reset();
        return isItABigStream;
    }

    @Override
    public BucketName getDefaultBucketName() {
        Preconditions.checkState(
            lowCostBlobStore.getDefaultBucketName()
                .equals(performingBlobStore.getDefaultBucketName()),
            "currentBlobStore and legacyBlobStore doen't have same defaultBucketName which could lead to " +
                "unexpected result when interact with other APIs");

        return lowCostBlobStore.getDefaultBucketName();
    }

    @Override
    public Mono<byte[]> readBytes(BucketName bucketName, BlobId blobId) {
        return Mono.defer(() -> performingBlobStore.readBytes(bucketName, blobId))
            .onErrorResume(this::logAndReturnEmpty)
            .switchIfEmpty(Mono.defer(() -> lowCostBlobStore.readBytes(bucketName, blobId)));
    }

    @Override
    public InputStream read(BucketName bucketName, BlobId blobId) {
        try {
            return performingBlobStore.read(bucketName, blobId);
        } catch (ObjectNotFoundException e) {
            return lowCostBlobStore.read(bucketName, blobId);
        } catch (Exception e) {
            LOGGER.error("Error reading {} {} in {}, falling back to {}", bucketName, blobId, performingBlobStore, lowCostBlobStore);
            return lowCostBlobStore.read(bucketName, blobId);
        }
    }

    @Override
    public Mono<Void> deleteBucket(BucketName bucketName) {
        return Mono.defer(() -> lowCostBlobStore.deleteBucket(bucketName))
            .and(performingBlobStore.deleteBucket(bucketName))
            .onErrorResume(this::logDeleteFailureAndReturnEmpty);
    }

    @Override
    public Mono<Void> delete(BucketName bucketName, BlobId blobId) {
        return Mono.defer(() -> lowCostBlobStore.delete(bucketName, blobId))
            .and(performingBlobStore.delete(bucketName, blobId))
            .onErrorResume(this::logDeleteFailureAndReturnEmpty);
    }

    private <T> Mono<T> logAndReturnEmpty(Throwable throwable) {
        LOGGER.error("error happens from current blob store, fall back to lowCost blob store", throwable);
        return Mono.empty();
    }

    private <T> Mono<T> logDeleteFailureAndReturnEmpty(Throwable throwable) {
        LOGGER.error("Cannot delete from either lowCost or performing blob store", throwable);
        return Mono.empty();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("lowCostBlobStore", lowCostBlobStore)
            .add("performingBlobStore", performingBlobStore)
            .toString();
    }
}