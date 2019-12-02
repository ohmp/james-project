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

package org.apache.james.mailbox.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Attachment {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AttachmentId attachmentId;
        private String type;
        private Long size;

        public Builder attachmentId(AttachmentId attachmentId) {
            Preconditions.checkArgument(attachmentId != null);
            this.attachmentId = attachmentId;
            return this;
        }

        public Builder type(String type) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(type));
            this.type = type;
            return this;
        }

        public Builder size(long size) {
            Preconditions.checkArgument(size >= 0, "'size' needs to be positive");
            this.size = size;
            return this;
        }

        public Attachment build() {
            Preconditions.checkState(type != null, "'type' is mandatory");
            Preconditions.checkState(size != null, "'size' is mandatory");
            AttachmentId builtAttachmentId = attachmentId();
            Preconditions.checkState(builtAttachmentId != null, "'attachmentId' is mandatory");

            return new Attachment(builtAttachmentId, type, size);
        }

        public Attachment.WithBytes buildWithBytes(byte[] bytes) {
            return size(bytes.length)
                .build()
                .withBytes(bytes);
        }

        private AttachmentId attachmentId() {
            if (attachmentId != null) {
                return attachmentId;
            }
            return AttachmentId.random();
        }
    }

    public static class WithBytes {
        private final byte[] bytes;
        private final Attachment metadata;

        public WithBytes(byte[] bytes, Attachment metadata) {
            Preconditions.checkArgument(bytes != null);
            this.bytes = bytes;
            this.metadata = metadata;
        }

        public Attachment getMetadata() {
            return metadata;
        }

        public InputStream getStream() {
            return new ByteArrayInputStream(bytes);
        }

        /**
         * Be careful the returned array is not a copy of the attachment byte array.
         * Mutating it will mutate the attachment!
         * @return the attachment content
         */
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof WithBytes) {
                WithBytes withBytes = (WithBytes) o;

                return Arrays.equals(this.bytes, withBytes.bytes)
                    && Objects.equals(this.metadata, withBytes.metadata);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(bytes, metadata);
        }
    }

    private final AttachmentId attachmentId;
    private final String type;
    private final long size;

    private Attachment(AttachmentId attachmentId, String type, long size) {
        this.attachmentId = attachmentId;
        this.type = type;
        this.size = size;
    }

    public AttachmentId getAttachmentId() {
        return attachmentId;
    }

    public String getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public Attachment.WithBytes withBytes(byte[] bytes) {
        Preconditions.checkNotNull(bytes);
        Preconditions.checkArgument(bytes.length == size, "Provided content do not match attachment size");
        return new WithBytes(bytes, this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attachment) {
            Attachment other = (Attachment) obj;
            return Objects.equals(attachmentId, other.attachmentId)
                && Objects.equals(type, other.type)
                && Objects.equals(size, other.size);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachmentId, type, size);
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("attachmentId", attachmentId)
                .add("type", type)
                .add("size", size)
                .toString();
    }
}
