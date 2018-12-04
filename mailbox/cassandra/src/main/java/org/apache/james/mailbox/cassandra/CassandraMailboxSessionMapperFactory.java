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

package org.apache.james.mailbox.cassandra;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.init.configuration.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.cassandra.mail.CassandraACLMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraAnnotationMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraApplicableFlagDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentDAOV2;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraAttachmentOwnerDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraDeletedMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraFirstUnseenDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraIndexTableHandler;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxCounterDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathDAOImpl;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxPathV2DAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMailboxRecentsDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageIdToImapUidDAO;
import org.apache.james.mailbox.cassandra.mail.CassandraMessageMapper;
import org.apache.james.mailbox.cassandra.mail.CassandraModSeqProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUidProvider;
import org.apache.james.mailbox.cassandra.mail.CassandraUserMailboxRightsDAO;
import org.apache.james.mailbox.cassandra.user.CassandraSubscriptionMapper;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.AttachmentMapperFactory;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

import com.datastax.driver.core.Session;

/**
 * Cassandra implementation of {@link MailboxSessionMapperFactory}
 */
public class CassandraMailboxSessionMapperFactory extends MailboxSessionMapperFactory implements AttachmentMapperFactory {
    protected static final String ATTACHMENTMAPPER = "ATTACHMENTMAPPER";

    private final Session session;
    private final CassandraUidProvider uidProvider;
    private final CassandraModSeqProvider modSeqProvider;
    private final CassandraAttachmentDAO attachmentDAO;
    private final CassandraAttachmentDAOV2 attachmentDAOV2;
    private final BlobStore blobStore;
    private final CassandraAttachmentMessageIdDAO attachmentMessageIdDAO;
    private final CassandraAttachmentOwnerDAO ownerDAO;

    private final CassandraMessageMapper cassandraMessageMapper;
    private final CassandraMailboxMapper cassandraMailboxMapper;
    private final CassandraSubscriptionMapper cassandraSubscriptionMapper;
    private final CassandraMessageIdMapper cassandraMessageIdMapper;
    private final CassandraAnnotationMapper cassandraAnnotationMapper;

    @Inject
    public CassandraMailboxSessionMapperFactory(CassandraUidProvider uidProvider, CassandraModSeqProvider modSeqProvider, Session session,
                                                CassandraMessageDAO messageDAO,
                                                CassandraMessageIdDAO messageIdDAO, CassandraMessageIdToImapUidDAO imapUidDAO,
                                                CassandraMailboxCounterDAO mailboxCounterDAO, CassandraMailboxRecentsDAO mailboxRecentsDAO, CassandraMailboxDAO mailboxDAO,
                                                CassandraMailboxPathDAOImpl mailboxPathDAO, CassandraMailboxPathV2DAO mailboxPathV2DAO, CassandraFirstUnseenDAO firstUnseenDAO, CassandraApplicableFlagDAO applicableFlagDAO,
                                                CassandraAttachmentDAO attachmentDAO, CassandraAttachmentDAOV2 attachmentDAOV2, CassandraDeletedMessageDAO deletedMessageDAO,
                                                BlobStore blobStore, CassandraAttachmentMessageIdDAO attachmentMessageIdDAO,
                                                CassandraAttachmentOwnerDAO ownerDAO, CassandraACLMapper aclMapper,
                                                CassandraUserMailboxRightsDAO userMailboxRightsDAO,
                                                CassandraUtils cassandraUtils, CassandraConfiguration cassandraConfiguration) {
        this.uidProvider = uidProvider;
        this.modSeqProvider = modSeqProvider;
        this.session = session;
        this.attachmentDAO = attachmentDAO;
        this.attachmentDAOV2 = attachmentDAOV2;
        this.blobStore = blobStore;
        this.attachmentMessageIdDAO = attachmentMessageIdDAO;
        this.ownerDAO = ownerDAO;
        CassandraIndexTableHandler indexTableHandler = new CassandraIndexTableHandler(
            mailboxRecentsDAO,
            mailboxCounterDAO,
            firstUnseenDAO,
            applicableFlagDAO,
            deletedMessageDAO);

        cassandraMailboxMapper = new CassandraMailboxMapper(mailboxDAO, mailboxPathDAO, mailboxPathV2DAO, userMailboxRightsDAO, aclMapper);

        cassandraMessageMapper = new CassandraMessageMapper(
            uidProvider,
            modSeqProvider,
            createAttachmentMapper(null),
            messageDAO,
            messageIdDAO,
            imapUidDAO,
            mailboxCounterDAO,
            mailboxRecentsDAO,
            applicableFlagDAO,
            indexTableHandler,
            firstUnseenDAO,
            deletedMessageDAO,
            cassandraConfiguration);

        cassandraSubscriptionMapper = new CassandraSubscriptionMapper(session, cassandraUtils);

        cassandraAnnotationMapper = new CassandraAnnotationMapper(session, cassandraUtils);

        cassandraMessageIdMapper = new CassandraMessageIdMapper(getMailboxMapper(), mailboxDAO,
            createAttachmentMapper(null),
            imapUidDAO, messageIdDAO, messageDAO, indexTableHandler, modSeqProvider,
            cassandraConfiguration);
    }


    @Override
    public MessageMapper getMessageMapper() {
        return cassandraMessageMapper;
    }

    @Override
    public MessageIdMapper getMessageIdMapper() {
        return cassandraMessageIdMapper;
    }

    @Override
    public MailboxMapper getMailboxMapper() {
        return cassandraMailboxMapper;
    }

    @Override
    public CassandraAttachmentMapper createAttachmentMapper(MailboxSession mailboxSession) {
        return new CassandraAttachmentMapper(attachmentDAO, attachmentDAOV2, blobStore, attachmentMessageIdDAO, ownerDAO);
    }

    @Override
    public SubscriptionMapper getSubscriptionMapper() {
        return cassandraSubscriptionMapper;
    }

    @Override
    public ModSeqProvider getModSeqProvider() {
        return modSeqProvider;
    }

    @Override
    public UidProvider getUidProvider() {
        return uidProvider;
    }

    Session getSession() {
        return session;
    }

    @Override
    public AnnotationMapper getAnnotationMapper() {
        return cassandraAnnotationMapper;
    }

    @Override
    public AttachmentMapper getAttachmentMapper(MailboxSession session) {
        AttachmentMapper mapper = (AttachmentMapper) session.getAttributes().get(ATTACHMENTMAPPER);
        if (mapper == null) {
            mapper = createAttachmentMapper(session);
            session.getAttributes().put(ATTACHMENTMAPPER, mapper);
        }
        return mapper;
    }
}
