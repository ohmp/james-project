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
package org.apache.james.mailbox.inmemory.mail;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.james.core.Username;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class InMemoryAttachmentMapper implements AttachmentMapper {

    private static class AttachmentWithBytes {
        private final byte[] bytes;
        private final Attachment metadata;

        private AttachmentWithBytes(byte[] bytes, Attachment metadata) {
            Preconditions.checkArgument(bytes != null);
            Preconditions.checkArgument(metadata.getSize() == bytes.length);
            this.bytes = bytes;
            this.metadata = metadata;
        }

        private Attachment getMetadata() {
            return metadata;
        }

        /**
         * Be careful the returned array is not a copy of the attachment byte array.
         * Mutating it will mutate the attachment!
         * @return the attachment content
         */
        private byte[] getBytes() {
            return bytes;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof AttachmentWithBytes) {
                AttachmentWithBytes withBytes = (AttachmentWithBytes) o;

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

    private static final int INITIAL_SIZE = 128;

    private final Map<AttachmentId, AttachmentWithBytes> attachmentsById;
    private final Multimap<AttachmentId, MessageId> messageIdsByAttachmentId;
    private final Multimap<AttachmentId, Username> ownersByAttachmentId;

    public InMemoryAttachmentMapper() {
        attachmentsById = new ConcurrentHashMap<>(INITIAL_SIZE);
        messageIdsByAttachmentId = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        ownersByAttachmentId = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    @Override
    public Attachment getAttachment(AttachmentId attachmentId) throws AttachmentNotFoundException {
        return retrieveContentWithBytes(attachmentId).getMetadata();
    }

    @Override
    public List<Attachment> getAttachments(Collection<AttachmentId> attachmentIds) {
        Preconditions.checkArgument(attachmentIds != null);
        Builder<Attachment> builder = ImmutableList.builder();
        for (AttachmentId attachmentId : attachmentIds) {
            if (attachmentsById.containsKey(attachmentId)) {
                builder.add(attachmentsById.get(attachmentId).getMetadata());
            }
        }
        return builder.build();
    }

    @Override
    public void storeAttachmentForOwner(Attachment.WithBytes attachment, Username owner) throws MailboxException {
        attachmentsById.put(attachment.getMetadata().getAttachmentId(), new AttachmentWithBytes(attachment.getBytes(), attachment.getMetadata()));
        ownersByAttachmentId.put(attachment.getMetadata().getAttachmentId(), owner);
    }

    @Override
    public void endRequest() {
        // Do nothing
    }

    @Override
    public <T> T execute(Transaction<T> transaction) throws MailboxException {
        return transaction.run();
    }

    @Override
    public void storeAttachmentsForMessage(Map<Attachment, byte[]> attachments, MessageId ownerMessageId) throws MailboxException {
        for (Map.Entry<Attachment, byte[]> attachment: attachments.entrySet()) {
            attachmentsById.put(attachment.getKey().getAttachmentId(), new AttachmentWithBytes(attachment.getValue(), attachment.getKey()));
            messageIdsByAttachmentId.put(attachment.getKey().getAttachmentId(), ownerMessageId);
        }
    }

    @Override
    public Collection<MessageId> getRelatedMessageIds(AttachmentId attachmentId) throws MailboxException {
        return messageIdsByAttachmentId.get(attachmentId);
    }

    @Override
    public Collection<Username> getOwners(final AttachmentId attachmentId) throws MailboxException {
        return ownersByAttachmentId.get(attachmentId);
    }

    @Override
    public ByteArrayInputStream retrieveContent(AttachmentId attachmentId) throws MailboxException {
        return new ByteArrayInputStream(retrieveContentWithBytes(attachmentId).getBytes());
    }

    private AttachmentWithBytes retrieveContentWithBytes(AttachmentId attachmentId) throws AttachmentNotFoundException {
        Preconditions.checkArgument(attachmentId != null);
        if (!attachmentsById.containsKey(attachmentId)) {
            throw new AttachmentNotFoundException(attachmentId.getId());
        }
        return attachmentsById.get(attachmentId);
    }
}