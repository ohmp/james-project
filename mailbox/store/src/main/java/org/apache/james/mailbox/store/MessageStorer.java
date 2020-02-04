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

package org.apache.james.mailbox.store;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.mail.Flags;
import javax.mail.internet.SharedInputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.ParsedAttachment;
import org.apache.james.mailbox.store.mail.AttachmentMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public interface MessageStorer {
    /**
     * If applicable, this method will parse the messageContent to retrieve associated attachments and will store them.
     */
    Pair<MessageMetaData, Optional<List<MessageAttachment>>> appendMessageToStore(Mailbox mailbox, Date internalDate, int size, int bodyStartOctet, SharedInputStream content, Flags flags, PropertyBuilder propertyBuilder, MailboxSession session) throws MailboxException;

    class WithAttachment implements MessageStorer {
        private static final Logger LOGGER = LoggerFactory.getLogger(WithAttachment.class);

        private final MailboxSessionMapperFactory mapperFactory;
        private final MessageId.Factory messageIdFactory;
        private final MessageFactory messageFactory;
        private final AttachmentMapperFactory attachmentMapperFactory;
        private final MessageParser messageParser;

        public WithAttachment(MailboxSessionMapperFactory mapperFactory, MessageId.Factory messageIdFactory,
                              MessageFactory messageFactory, AttachmentMapperFactory attachmentMapperFactory,
                              MessageParser messageParser) {
            this.mapperFactory = mapperFactory;
            this.messageIdFactory = messageIdFactory;
            this.messageFactory = messageFactory;
            this.attachmentMapperFactory = attachmentMapperFactory;
            this.messageParser = messageParser;
        }

        @Override
        public Pair<MessageMetaData, Optional<List<MessageAttachment>>> appendMessageToStore(Mailbox mailbox, Date internalDate, int size, int bodyStartOctet, SharedInputStream content, Flags flags, PropertyBuilder propertyBuilder, MailboxSession session) throws MailboxException {
            MessageMapper messageMapper = mapperFactory.getMessageMapper(session);
            MessageId messageId = messageIdFactory.generate();

            return mapperFactory.getMessageMapper(session).execute(() -> {
                List<MessageAttachment> attachments = storeAttachments(messageId, content, session);
                MailboxMessage message = messageFactory.createMessage(messageId, mailbox, internalDate, size, bodyStartOctet, content, flags, propertyBuilder, attachments);
                MessageMetaData metadata = messageMapper.add(mailbox, message);
                return Pair.of(metadata, Optional.of(attachments));
            });
        }

        private List<MessageAttachment> storeAttachments(MessageId messageId, SharedInputStream messageContent, MailboxSession session) throws MailboxException {
            List<ParsedAttachment> attachments = extractAttachments(messageContent);
            return attachmentMapperFactory.getAttachmentMapper(session)
                .storeAttachmentsForMessage(attachments, messageId);
        }

        private List<ParsedAttachment> extractAttachments(SharedInputStream contentIn) {
            try {
                return messageParser.retrieveAttachments(contentIn.newStream(0, -1));
            } catch (Exception e) {
                LOGGER.warn("Error while parsing mail's attachments: {}", e.getMessage(), e);
                return ImmutableList.of();
            }
        }
    }

    class WithoutAttachment implements MessageStorer {
        private final MailboxSessionMapperFactory mapperFactory;
        private final MessageId.Factory messageIdFactory;
        private final MessageFactory messageFactory;

        public WithoutAttachment(MailboxSessionMapperFactory mapperFactory, MessageId.Factory messageIdFactory, MessageFactory messageFactory) {
            this.mapperFactory = mapperFactory;
            this.messageIdFactory = messageIdFactory;
            this.messageFactory = messageFactory;
        }

        @Override
        public Pair<MessageMetaData, Optional<List<MessageAttachment>>> appendMessageToStore(Mailbox mailbox, Date internalDate, int size, int bodyStartOctet, SharedInputStream content, Flags flags, PropertyBuilder propertyBuilder, MailboxSession session) throws MailboxException {
            MessageMapper messageMapper = mapperFactory.getMessageMapper(session);
            MessageId messageId = messageIdFactory.generate();

            return mapperFactory.getMessageMapper(session).execute(() -> {
                MailboxMessage message = messageFactory.createMessage(messageId, mailbox, internalDate, size, bodyStartOctet, content, flags, propertyBuilder, ImmutableList.of());
                MessageMetaData metadata = messageMapper.add(mailbox, message);
                return Pair.of(metadata, Optional.empty());
            });
        }
    }
}
