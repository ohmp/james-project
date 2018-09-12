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

package org.apache.james.queue.rabbitmq.helper.cassandra.model;

import java.time.Instant;
import java.util.Objects;

import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.mailet.Mail;

import com.google.common.base.Preconditions;

public class EnqueuedMail {

    public interface Builder {
        @FunctionalInterface
        interface RequireQueueName {
            RequireMail mailQueueName(MailQueueName mailQueueName);
        }

        @FunctionalInterface
        interface RequireMail {
            RequireSlice forMail(Mail mail);
        }

        @FunctionalInterface
        interface RequireSlice {
            RequireBucket timeRangeStart(Instant timeRangeStart);
        }

        @FunctionalInterface
        interface RequireBucket {
            LastStage bucketId(Integer bucketId);
        }

        class LastStage {
            private final Mail mail;
            private final int bucketId;
            private final Instant timeRangeStart;
            private final MailQueueName mailQueueName;

            private LastStage(Mail mail, int bucketId, Instant timeRangeStart, MailQueueName mailQueueName) {
                this.mail = mail;
                this.bucketId = bucketId;
                this.timeRangeStart = timeRangeStart;
                this.mailQueueName = mailQueueName;
            }

            public EnqueuedMail build() {
                Preconditions.checkNotNull(mail, "'mail' is mandatory");
                Preconditions.checkNotNull(timeRangeStart, "'timeRangeStart' is mandatory");
                Preconditions.checkNotNull(mailQueueName, "'mailQueueName' is mandatory");
                Preconditions.checkState(bucketId >= 0, "'bucketId' needs to be positive");

                return new EnqueuedMail(
                    mail,
                    bucketId,
                    timeRangeStart,
                    MailKey.fromMail(mail),
                    mailQueueName);
            }
        }
    }

    public static Builder.RequireQueueName builder() {
        return queueName -> mail -> sliceStart -> bucketId -> new Builder.LastStage(mail, bucketId, sliceStart, queueName);
    }

    private final Mail mail;
    private final int bucketId;
    private final Instant timeRangeStart;
    private final MailKey mailKey;
    private final MailQueueName mailQueueName;

    private EnqueuedMail(Mail mail, int bucketId, Instant timeRangeStart, MailKey mailKey, MailQueueName mailQueueName) {
        this.mail = mail;
        this.bucketId = bucketId;
        this.timeRangeStart = timeRangeStart;
        this.mailKey = mailKey;
        this.mailQueueName = mailQueueName;
    }

    public Mail getMail() {
        return mail;
    }

    public int getBucketId() {
        return bucketId;
    }

    public MailKey getMailKey() {
        return mailKey;
    }

    public MailQueueName getMailQueueName() {
        return mailQueueName;
    }

    public Instant getTimeRangeStart() {
        return timeRangeStart;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof EnqueuedMail) {
            EnqueuedMail that = (EnqueuedMail) o;

            return Objects.equals(this.bucketId, that.bucketId)
                    && Objects.equals(this.mail, that.mail)
                    && Objects.equals(this.timeRangeStart, that.timeRangeStart)
                    && Objects.equals(this.mailKey, that.mailKey)
                    && Objects.equals(this.mailQueueName, that.mailQueueName);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(mail, bucketId, timeRangeStart, mailKey, mailQueueName);
    }
}
