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

package org.apache.james.queue.rabbitmq.helper.cassandra;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.helper.cassandra.model.BucketedSlices;
import org.apache.james.queue.rabbitmq.helper.cassandra.model.EnqueuedMail;
import org.apache.james.queue.rabbitmq.helper.cassandra.model.MailKey;
import org.apache.james.util.FluentFutureStream;
import org.apache.mailet.Mail;

class DeleteMailHelper {

    private final DeletedMailsDAO deletedMailsDao;
    private final EnqueuedMailsDAO enqueuedMailsDao;
    private final BrowseStartDAO browseStartDao;
    private final CassandraRabbitMQConfiguration configuration;
    private final Random random;

    @Inject
    DeleteMailHelper(DeletedMailsDAO deletedMailsDao,
                            EnqueuedMailsDAO enqueuedMailsDao,
                            BrowseStartDAO browseStartDao,
                            CassandraRabbitMQConfiguration configuration) {
        this.deletedMailsDao = deletedMailsDao;
        this.enqueuedMailsDao = enqueuedMailsDao;
        this.browseStartDao = browseStartDao;

        this.configuration = configuration;
        this.random = new Random();
    }

    CompletableFuture<Void> updateDeleteTable(Mail mail, MailQueueName mailQueueName) {
        return deletedMailsDao
            .markAsDeleted(mailQueueName, MailKey.fromMail(mail))
            .thenCompose(avoid -> updateBrowseStart(mailQueueName));
    }

    private CompletableFuture<Void> updateBrowseStart(MailQueueName mailQueueName) {
        if (shouldBrowseStart()) {
            return browseStartDao
                .findBrowseStart(mailQueueName)
                .thenCompose(oldBrowseStart -> findNewBrowseStart(mailQueueName, oldBrowseStart))
                .thenCompose(newBrowseStart -> setNewBrowseStart(mailQueueName, newBrowseStart));
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> setNewBrowseStart(MailQueueName mailQueueName, Optional<Instant> newBrowseStart) {
        return newBrowseStart.map(value ->
            browseStartDao.updateBrowseStart(mailQueueName, value))
            .orElse(CompletableFuture.completedFuture(null));
    }

    private CompletableFuture<Optional<Instant>> findNewBrowseStart(
        MailQueueName mailQueueName, Optional<Instant> sliceStart) {

        Stream<BucketedSlices.BucketAndSlice> allBucketSlices = calculateBucketedSlices(sliceStart.get());
        // todo handle not yet browseStart references

        return FluentFutureStream
            .ofNestedStreams(allBucketSlices
                .map(bucketAndSlice -> enqueuedMailsDao.selectEnqueuedMails(mailQueueName, bucketAndSlice.getBucketId(), bucketAndSlice.getSliceStartInstant())))
            .thenFlatComposeOnOptional(mailReference -> filterDeleted(mailQueueName, mailReference))
            .map(EnqueuedMail::getTimeRangeStart)
            .completableFuture()
            .thenApply(Stream::findFirst);
    }

    private CompletableFuture<Optional<EnqueuedMail>> filterDeleted(MailQueueName mailQueueName, EnqueuedMail mailReference) {
        return deletedMailsDao.checkDeleted(mailQueueName, mailReference.getMailKey())
            .thenApply(wasDeleted ->
                Optional.of(mailReference)
                    .filter(any -> !wasDeleted));
    }

    private boolean shouldBrowseStart() {
        int threshold = configuration.getUpdateFirstEnqueuedPace();
        return Math.abs(random.nextInt()) % threshold == 0;
    }

    private Stream<BucketedSlices.BucketAndSlice> calculateBucketedSlices(Instant startingPoint) {
        return BucketedSlices.builder()
            .startAt(startingPoint)
            .bucketCount(configuration.getBucketCount())
            .sliceWindowSideInSecond(configuration.getSliceWindow().getSeconds())
            .build()
            .getBucketSlices();
    }
}
