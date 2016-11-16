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
package org.apache.james.mailbox.cassandra.mail;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.mail.Flags;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.ComposedMessageIdWithMetaData;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.FlagsUpdateCalculator;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class CassandraMessageIdMapper implements MessageIdMapper {

    public static final boolean DEFAULT_STOP_IF_DELETED = true;
    private final MailboxMapper mailboxMapper;
    private final AttachmentMapper attachmentMapper;
    private final CassandraMessageIdToImapUidDAO imapUidDAO;
    private final CassandraMessageIdDAO messageIdDAO;
    private final CassandraMessageDAO messageDAO;
    private final ModSeqProvider modSeqProvider;
    private final MailboxSession mailboxSession;

    public CassandraMessageIdMapper(MailboxMapper mailboxMapper, AttachmentMapper attachmentMapper,
                                    CassandraMessageIdToImapUidDAO imapUidDAO, CassandraMessageIdDAO messageIdDAO,
                                    CassandraMessageDAO messageDAO, ModSeqProvider modSeqProvider, MailboxSession mailboxSession) {
        this.mailboxMapper = mailboxMapper;
        this.attachmentMapper = attachmentMapper;
        this.imapUidDAO = imapUidDAO;
        this.messageIdDAO = messageIdDAO;
        this.messageDAO = messageDAO;
        this.modSeqProvider = modSeqProvider;
        this.mailboxSession = mailboxSession;
    }

    @Override
    public List<MailboxMessage> find(List<MessageId> messageIds, FetchType fetchType) {
        return findAsStream(messageIds, fetchType)
            .collect(Guavate.toImmutableList());
    }

    private Stream<SimpleMailboxMessage> findAsStream(List<MessageId> messageIds, FetchType fetchType) {
        List<ComposedMessageIdWithMetaData> composedMessageIds = messageIds.stream()
            .map(messageId -> imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty()))
            .flatMap(CompletableFuture::join)
            .collect(Guavate.toImmutableList());
        return messageDAO.retrieveMessages(composedMessageIds, fetchType, Optional.empty()).join()
            .map(loadAttachments())
            .map(toMailboxMessages())
            .sorted(Comparator.comparing(MailboxMessage::getUid));
    }

    private Function<Pair<MailboxMessage, Stream<MessageAttachmentById>>, Pair<MailboxMessage, Stream<MessageAttachment>>> loadAttachments() {
        return pair -> Pair.of(pair.getLeft(), new AttachmentLoader(attachmentMapper).getAttachments(pair.getRight().collect(Guavate.toImmutableList())));
    }

    private FunctionChainer<Pair<MailboxMessage, Stream<MessageAttachment>>, SimpleMailboxMessage> toMailboxMessages() {
        return Throwing.function(pair -> SimpleMailboxMessage.cloneWithAttachments(
            pair.getLeft(),
            pair.getRight().collect(Guavate.toImmutableList())));
    }

    @Override
    public List<MailboxId> findMailboxes(MessageId messageId) {
        return imapUidDAO.retrieve((CassandraMessageId) messageId, Optional.empty()).join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .map(ComposedMessageId::getMailboxId)
            .collect(Guavate.toImmutableList());
    }

    @Override
    public void save(MailboxMessage mailboxMessage) throws MailboxNotFoundException, MailboxException {
        CassandraId mailboxId = (CassandraId) mailboxMessage.getMailboxId();
        messageDAO.save(mailboxMapper.findMailboxById(mailboxId), mailboxMessage).join();
        CassandraMessageId messageId = (CassandraMessageId) mailboxMessage.getMessageId();
        ComposedMessageIdWithMetaData composedMessageIdWithMetaData = ComposedMessageIdWithMetaData.builder()
            .composedMessageId(new ComposedMessageId(mailboxId, messageId, mailboxMessage.getUid()))
            .flags(mailboxMessage.createFlags())
            .modSeq(mailboxMessage.getModSeq())
            .build();
        CompletableFuture.allOf(imapUidDAO.insert(composedMessageIdWithMetaData),
            messageIdDAO.insert(composedMessageIdWithMetaData))
            .join();
    }

    @Override
    public void delete(MessageId messageId, List<MailboxId> mailboxIds) {
        CassandraMessageId cassandraMessageId = (CassandraMessageId) messageId;

        CompletableFuture<Void> imapUiFuture = deleteInMessageIdToMailboxMapping(mailboxIds, cassandraMessageId);
        List<ComposedMessageId> composedMessageIds = retrieveMessageIds(cassandraMessageId);
        CompletableFuture<Void> messageIdFuture = deleteInMailboxToMessageIdMapping(mailboxIds, composedMessageIds);
        CompletableFuture<Void> messageFuture = deleteInMessageTable(mailboxIds, cassandraMessageId, composedMessageIds);

        CompletableFuture.allOf(imapUiFuture, messageFuture, messageIdFuture);
    }

    private CompletableFuture<Void> deleteInMessageTable(List<MailboxId> mailboxIds, CassandraMessageId cassandraMessageId, List<ComposedMessageId> composedMessageIds) {
        return composedMessageIds.stream()
            .filter(composedMessageId -> !isInSpecifiedList(mailboxIds, composedMessageId))
            .findAny()
            .map(any -> CompletableFuture.completedFuture((Void) null))
            .orElse(messageDAO.delete(cassandraMessageId));
    }

    private CompletableFuture<Void> deleteInMailboxToMessageIdMapping(List<MailboxId> mailboxIds, List<ComposedMessageId> composedMessageIds) {
        return composedMessageIds.stream()
            .filter(composedMessageId -> isInSpecifiedList(mailboxIds, composedMessageId))
            .map(composedMessageId -> messageIdDAO.delete((CassandraId) composedMessageId.getMailboxId(), composedMessageId.getUid()))
            .reduce(CompletableFuture::allOf)
            .orElse(CompletableFuture.completedFuture(null));
    }

    private CompletableFuture<Void> deleteInMessageIdToMailboxMapping(List<MailboxId> mailboxIds, CassandraMessageId cassandraMessageId) {
        return mailboxIds.stream()
            .map(mailboxId -> imapUidDAO.delete(cassandraMessageId, (CassandraId) mailboxId))
            .reduce(CompletableFuture::allOf)
            .orElse(CompletableFuture.completedFuture(null));
    }

    private boolean isInSpecifiedList(List<MailboxId> mailboxIds, ComposedMessageId composedMessageId) {
        return mailboxIds.contains(composedMessageId.getMailboxId());
    }

    private List<ComposedMessageId> retrieveMessageIds(CassandraMessageId cassandraMessageId) {
        return imapUidDAO.retrieve(cassandraMessageId, Optional.empty())
            .join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .collect(Guavate.toImmutableList());
    }

    @Override
    public void delete(MessageId messageId) {
        CassandraMessageId cassandraMessageId = (CassandraMessageId) messageId;
        messageDAO.delete(cassandraMessageId).join();
        imapUidDAO.retrieve(cassandraMessageId, Optional.empty()).join()
            .map(ComposedMessageIdWithMetaData::getComposedMessageId)
            .forEach(composedMessageId -> deleteIds(composedMessageId).join());
    }

    private CompletableFuture<Void> deleteIds(ComposedMessageId composedMessageId) {
        CassandraMessageId messageId = (CassandraMessageId) composedMessageId.getMessageId();
        CassandraId mailboxId = (CassandraId) composedMessageId.getMailboxId();
        return CompletableFuture.allOf(imapUidDAO.delete(messageId, mailboxId),
            messageIdDAO.delete(mailboxId, composedMessageId.getUid()));
    }

    @Override
    public void setFlags(Flags newState, MessageManager.FlagsUpdateMode replace, MessageId messageId, Mailbox mailbox) throws MailboxException {
        long oldModSeq = modSeqProvider.highestModSeq(mailboxSession, mailbox);
        new FunctionRunnerWithRetry(1000)
            .execute(() -> tryFlagsUpdate(newState, replace, messageId, mailbox, oldModSeq));
    }

    private Boolean tryFlagsUpdate(Flags newState, MessageManager.FlagsUpdateMode replace, MessageId messageId, Mailbox mailbox, long oldModSeq) throws MailboxException {
        long newModSeq = modSeqProvider.nextModSeq(mailboxSession, mailbox);
        return findAsStream(ImmutableList.of(messageId), FetchType.Metadata)
                .findAny()
                .map(metadata -> new ComposedMessageIdWithMetaData(
                    new ComposedMessageId(mailbox.getMailboxId(), messageId, metadata.getUid()),
                    new FlagsUpdateCalculator(metadata.createFlags(), replace).buildNewFlags(newState),
                    newModSeq))
                .map(composedMessageIdWithMetaData -> imapUidDAO.updateMetadata(composedMessageIdWithMetaData, oldModSeq).join())
                .orElse(DEFAULT_STOP_IF_DELETED);
    }
}
