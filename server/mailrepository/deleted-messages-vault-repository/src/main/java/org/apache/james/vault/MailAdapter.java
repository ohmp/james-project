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

package org.apache.james.vault;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.MessagingException;

import org.apache.james.core.User;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.server.core.MailImpl;
import org.apache.james.server.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.server.core.MimeMessageInputStreamSource;
import org.apache.james.util.MemoizedSupplier;
import org.apache.james.util.MimeMessageUtil;
import org.apache.james.util.OptionalUtils;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.Mail;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class MailAdapter {
    private static final AttributeName ORIGIN_MAILBOXES_ATTRIBUTE_NAME = AttributeName.of("originMailboxes");
    private static final AttributeName HAS_ATTACHMENT_ATTRIBUTE_NAME = AttributeName.of("hasAttachment");
    private static final AttributeName OWNER_ATTRIBUTE_NAME = AttributeName.of("owner");
    private static final AttributeName DELIVERY_DATE_ATTRIBUTE_NAME = AttributeName.of("deliveryDate");
    private static final AttributeName DELETION_DATE_ATTRIBUTE_NAME = AttributeName.of("deletionDate");
    private static final AttributeName SUBJECT_ATTRIBUTE_VALUE = AttributeName.of("subject");

    private final MailboxId.Factory mailboxIdFactory;
    private final MessageId.Factory messageIdFactory;

    public MailAdapter(MailboxId.Factory mailboxIdFactory, MessageId.Factory messageIdFactory) {
        this.mailboxIdFactory = mailboxIdFactory;
        this.messageIdFactory = messageIdFactory;
    }

    Mail toMail(DeletedMessage deletedMessage) throws MessagingException {
        MimeMessageCopyOnWriteProxy mimeMessage = new MimeMessageCopyOnWriteProxy(new MimeMessageInputStreamSource(deletedMessage.getMessageId().serialize(), deletedMessage.getContent().get()));

        Collection<AttributeValue<?>> serializedMailboxIds = deletedMessage.getOriginMailboxes().stream()
            .map(MailboxId::serialize)
            .map(AttributeValue::of)
            .collect(Guavate.toImmutableList());

        AttributeValue<?> subjectAttributeValue = AttributeValue.of(deletedMessage.getSubject().map(AttributeValue::of));

        return MailImpl.builder()
            .name(deletedMessage.getMessageId().serialize())
            .sender(deletedMessage.getSender())
            .recipients(deletedMessage.getRecipients())
            .mimeMessage(mimeMessage)
            .attribute(new Attribute(OWNER_ATTRIBUTE_NAME, AttributeValue.of(new SerializableUser(deletedMessage.getOwner()))))
            .attribute(new Attribute(HAS_ATTACHMENT_ATTRIBUTE_NAME, AttributeValue.of(deletedMessage.hasAttachment())))
            .attribute(new Attribute(DELIVERY_DATE_ATTRIBUTE_NAME, AttributeValue.of(new SerializableDate(deletedMessage.getDeliveryDate()))))
            .attribute(new Attribute(DELETION_DATE_ATTRIBUTE_NAME, AttributeValue.of(new SerializableDate(deletedMessage.getDeletionDate()))))
            .attribute(new Attribute(ORIGIN_MAILBOXES_ATTRIBUTE_NAME, AttributeValue.of(serializedMailboxIds)))
            .attribute(new Attribute(SUBJECT_ATTRIBUTE_VALUE, subjectAttributeValue))
            .build();
    }

    DeletedMessage fromMail(Mail mail) {
        MemoizedSupplier<byte[]> byteContent = new MemoizedSupplier<>(Throwing.supplier(
            () -> MimeMessageUtil.asBytes(mail.getMessage())));

        return DeletedMessage.builder()
            .messageId(messageIdFactory.fromString(mail.getName()))
            .originMailboxes(retrieveMailboxIds(mail))
            .user(retrieveOwner(mail))
            .deliveryDate(retrieveDate(mail, DELIVERY_DATE_ATTRIBUTE_NAME))
            .deletionDate(retrieveDate(mail, DELETION_DATE_ATTRIBUTE_NAME))
            .sender(mail.getMaybeSender())
            .recipients(ImmutableList.copyOf(mail.getRecipients()))
            .content(() -> new ByteArrayInputStream(byteContent.get()))
            .hasAttachment(retrieveHasAttachment(mail))
            .subject(retrieveSubject(mail))
            .build();

    }

    private Optional<String> retrieveSubject(Mail mail) {
        return mail.getAttribute(SUBJECT_ATTRIBUTE_VALUE)
                .map(Attribute::getValue)
                .map(AttributeValue::getValue)
                .filter(value -> value instanceof Optional)
                .map(value -> (Optional) value)
                .map(optional -> optional.map(obj -> {
                    Preconditions.checkArgument(obj instanceof String, "mail should have a 'subject' attribute being of type 'Optional<String>");
                    return (String) obj;
                }))
                .orElseThrow(() -> new IllegalArgumentException("mail should have a 'subject' attribute being of type 'Optional<String>"));
    }

    private boolean retrieveHasAttachment(Mail mail) {
        Attribute attribute = mail.getAttribute(HAS_ATTACHMENT_ATTRIBUTE_NAME).orElseThrow(() -> new IllegalArgumentException("mail should have a 'hasAttachment' attribute"));
        Preconditions.checkArgument(attribute.getValue().value() instanceof Boolean, "mail should have a 'hasAttachment' attribute of type boolean");
        return (boolean) (Boolean) attribute.getValue().value();
    }

    private ZonedDateTime retrieveDate(Mail mail, AttributeName attributeName) {
        Attribute attribute = mail.getAttribute(attributeName)
            .orElseThrow(() -> new IllegalArgumentException("'mail' should have a '" + attributeName.asString() + "' attribute"));

        Preconditions.checkArgument(attribute.getValue().getValue() instanceof SerializableDate, "'mail' should have a '" + attributeName.asString() + "' attribute of type SerializableDate");
        SerializableDate dateSerializable = (SerializableDate) attribute.getValue().value();
        return dateSerializable.getValue();
    }

    private User retrieveOwner(Mail mail) {
        Attribute ownerAttributes = mail.getAttribute(OWNER_ATTRIBUTE_NAME)
            .orElseThrow(() -> new IllegalArgumentException("Supplied email is missing the 'owner' field"));
        Preconditions.checkArgument(ownerAttributes.getValue().getValue() instanceof SerializableUser, "mail should have a 'owner' attribute of type SerializedUser");

        SerializableUser serializableUser = (SerializableUser) ownerAttributes.getValue().getValue();
        return serializableUser.getValue();
    }

    private List<MailboxId> retrieveMailboxIds(Mail mail) {
        Stream<AttributeValue> attributeValues = OptionalUtils.toStream(mail.getAttribute(ORIGIN_MAILBOXES_ATTRIBUTE_NAME))
            .map(Attribute::getValue)
            .map(AttributeValue::value)
            .peek(obj -> Preconditions.checkArgument(obj instanceof List, "'originMailboxes' shoould be of type list"))
            .map(obj -> (List) obj)
            .flatMap(list -> list.stream())
            .peek(obj -> Preconditions.checkArgument(obj instanceof AttributeValue, "'mailboxId' should be of type AttributeValue<String>. Found " + obj.getClass()))
            .map(obj -> (AttributeValue) obj);
        Stream<String> serializedMailboxIdStream = attributeValues.map(AttributeValue::value)
            .peek(obj -> Preconditions.checkArgument(obj instanceof String, "'mailboxId' should be of type AttributeValue<String>. Found " + obj.getClass()))
            .map(obj -> (String) obj);
        return serializedMailboxIdStream
            .map(mailboxIdFactory::fromString)
            .collect(Guavate.toImmutableList());
    }
}
