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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import org.apache.james.core.Username;
import org.apache.james.mailbox.exception.AttachmentNotFoundException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public abstract class AttachmentMapperTest {
    private static final AttachmentId UNKNOWN_ATTACHMENT_ID = AttachmentId.from("unknown");
    private static final Username OWNER = Username.of("owner");
    private static final Username ADDITIONAL_OWNER = Username.of("additionalOwner");
    private static final byte[] OTHER_BYTES = "something different".getBytes(StandardCharsets.UTF_8);

    private AttachmentMapper attachmentMapper;
    private static final byte[] BYTES = "payload".getBytes(StandardCharsets.UTF_8);
    private static final Attachment.WithBytes ATTACHMENT = Attachment.builder()
        .type("content")
        .buildWithBytes(BYTES);
    private static final Attachment.WithBytes OTHER_ATTACHMENT = Attachment.builder()
        .type("content")
        .buildWithBytes(OTHER_BYTES);

    protected abstract AttachmentMapper createAttachmentMapper();

    protected abstract MessageId generateMessageId();

    @BeforeEach
    void setUp() {
        this.attachmentMapper = createAttachmentMapper();
    }

    @Test
    void getAttachmentShouldThrowWhenNullAttachmentId() {
        assertThatThrownBy(() -> attachmentMapper.getAttachment(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAttachmentShouldThrowWhenNonReferencedAttachmentId() {
        assertThatThrownBy(() -> attachmentMapper.getAttachment(UNKNOWN_ATTACHMENT_ID))
            .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void retrieveContentShouldThrowWhenNonReferencedAttachmentId() {
        assertThatThrownBy(() -> attachmentMapper.retrieveContent(UNKNOWN_ATTACHMENT_ID))
            .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void getAttachmentShouldReturnTheAttachmentWhenReferenced() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);
        //When
        Attachment attachment = attachmentMapper.getAttachment(attachmentId);
        //Then
        assertThat(attachment).isEqualTo(ATTACHMENT.getMetadata());
    }

    @Test
    void retrieveContentShouldReturnTheAttachmentWhenReferenced() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);
        //When
        InputStream attachment = attachmentMapper.retrieveContent(attachmentId);
        //Then
        assertThat(attachment).hasSameContentAs(ATTACHMENT.getStream());
    }

    @Test
    void getAttachmentShouldReturnTheAttachmentsWhenMultipleStored() throws Exception {
        //Given
        byte[] bytes = "payload1".getBytes(StandardCharsets.UTF_8);
        byte[] bytes2 = "payload2".getBytes(StandardCharsets.UTF_8);
        Attachment.WithBytes expected1 = Attachment.builder()
                .type("content1")
                .buildWithBytes(bytes);
        Attachment.WithBytes expected2 = Attachment.builder()
                .type("content2")
                .buildWithBytes(bytes2);
        AttachmentId attachmentId1 = expected1.getMetadata().getAttachmentId();
        AttachmentId attachmentId2 = expected2.getMetadata().getAttachmentId();
        //When
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(expected1, expected2), generateMessageId());
        //Then
        Attachment attachment1 = attachmentMapper.getAttachment(attachmentId1);
        Attachment attachment2 = attachmentMapper.getAttachment(attachmentId2);
        assertThat(attachment1).isEqualTo(expected1.getMetadata());
        assertThat(attachment2).isEqualTo(expected2.getMetadata());
    }

    @Test
    void getAttachmentsShouldThrowWhenNullAttachmentId() {
        assertThatThrownBy(() -> attachmentMapper.getAttachments(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAttachmentsShouldReturnEmptyListWhenNonReferencedAttachmentId() {
        List<Attachment> attachments = attachmentMapper.getAttachments(ImmutableList.of(UNKNOWN_ATTACHMENT_ID));

        assertThat(attachments).isEmpty();
    }

    @Test
    void getAttachmentsShouldReturnTheAttachmentsWhenSome() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);

        byte[] bytes = "payload2".getBytes(StandardCharsets.UTF_8);
        Attachment.WithBytes expected2 = Attachment.builder()
                .type("content")
                .buildWithBytes(bytes);
        AttachmentId attachmentId2 = expected2.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(expected2, OWNER);

        //When
        List<Attachment> attachments = attachmentMapper.getAttachments(ImmutableList.of(attachmentId, attachmentId2));
        //Then
        assertThat(attachments).contains(ATTACHMENT.getMetadata(), expected2.getMetadata());
    }

    @Test
    void getOwnerMessageIdsShouldReturnEmptyWhenNone() throws Exception {
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(UNKNOWN_ATTACHMENT_ID);

        assertThat(messageIds).isEmpty();
    }

    @Test
    void getOwnerMessageIdsShouldReturnEmptyWhenStoredWithoutMessageId() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).isEmpty();
    }

    @Test
    void getOwnerMessageIdsShouldReturnMessageIdWhenStoredWithMessageId() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        MessageId messageId = generateMessageId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId);
    }

    @Test
    void getOwnerMessageIdsShouldReturnTwoMessageIdsWhenStoredTwice() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        MessageId messageId1 = generateMessageId();
        MessageId messageId2 = generateMessageId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId1);
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId2);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId1, messageId2);
    }

    @Test
    void getOwnerMessageIdsShouldReturnOnlyMatchingMessageId() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        MessageId messageId1 = generateMessageId();
        MessageId messageId2 = generateMessageId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId1);
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(OTHER_ATTACHMENT), messageId2);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId1);
    }

    @Test
    void getOwnerMessageIdsShouldReturnOnlyOneMessageIdWhenStoredTwice() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        MessageId messageId = generateMessageId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId);
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), messageId);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        //Then
        assertThat(messageIds).containsOnly(messageId);
    }

    @Test
    void getOwnerMessageIdsShouldReturnMessageIdForTwoAttachmentsWhenBothStoredAtTheSameTime() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        AttachmentId attachmentId2 = OTHER_ATTACHMENT.getMetadata().getAttachmentId();
        MessageId messageId = generateMessageId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT, OTHER_ATTACHMENT), messageId);
        
        //When
        Collection<MessageId> messageIds = attachmentMapper.getRelatedMessageIds(attachmentId);
        Collection<MessageId> messageIds2 = attachmentMapper.getRelatedMessageIds(attachmentId2);
        //Then
        assertThat(messageIds).isEqualTo(messageIds2);
    }

    @Test
    void getOwnersShouldBeRetrievedWhenExplicitlySpecified() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);

        //When
        Collection<Username> expectedOwners = ImmutableList.of(OWNER);
        Collection<Username> actualOwners = attachmentMapper.getOwners(attachmentId);
        //Then
        assertThat(actualOwners).containsOnlyElementsOf(expectedOwners);
    }

    @Test
    void getOwnersShouldReturnEmptyWhenMessageIdReferenced() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentsForMessage(ImmutableList.of(ATTACHMENT), generateMessageId());

        //When
        Collection<Username> actualOwners = attachmentMapper.getOwners(attachmentId);
        //Then
        assertThat(actualOwners).isEmpty();
    }

    @Test
    void getOwnersShouldReturnAllOwners() throws Exception {
        //Given
        AttachmentId attachmentId = ATTACHMENT.getMetadata().getAttachmentId();
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, OWNER);
        attachmentMapper.storeAttachmentForOwner(ATTACHMENT, ADDITIONAL_OWNER);

        //When
        Collection<Username> expectedOwners = ImmutableList.of(OWNER, ADDITIONAL_OWNER);
        Collection<Username> actualOwners = attachmentMapper.getOwners(attachmentId);
        //Then
        assertThat(actualOwners).containsOnlyElementsOf(expectedOwners);
    }

    @Test
    void getOwnersShouldReturnEmptyWhenUnknownAttachmentId() throws Exception {
        Collection<Username> actualOwners = attachmentMapper.getOwners(AttachmentId.from("any"));

        assertThat(actualOwners).isEmpty();
    }
}
