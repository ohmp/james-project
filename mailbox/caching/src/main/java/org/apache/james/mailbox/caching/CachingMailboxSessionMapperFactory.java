package org.apache.james.mailbox.caching;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

/**
 * A MailboxSessionMapperFactory that uses the underlying MailboxSessionMapperFactory to provide
 * caching variants of MessageMapper and MailboxMapper built around the MessageMapper and MailboxMapper
 * provided by it
 * 
 */
public class CachingMailboxSessionMapperFactory extends MailboxSessionMapperFactory {

    private final MailboxSessionMapperFactory underlying;
    private final MailboxByPathCache mailboxByPathCache;
    private final MailboxMetadataCache mailboxMetadataCache;

    public CachingMailboxSessionMapperFactory(MailboxSessionMapperFactory underlying, MailboxByPathCache mailboxByPathCache, MailboxMetadataCache mailboxMetadataCache) {
        this.underlying = underlying;
        this.mailboxByPathCache = mailboxByPathCache;
        this.mailboxMetadataCache = mailboxMetadataCache;
    }

    @Override
    public MailboxMapper getMailboxMapper() {
        return new CachingMailboxMapper(underlying.getMailboxMapper(), mailboxByPathCache);
    }

    @Override
    public MessageMapper getMessageMapper() {
        return new CachingMessageMapper(underlying.getMessageMapper(), mailboxMetadataCache);
    }

    @Override
    public SubscriptionMapper getSubscriptionMapper() throws SubscriptionException {
        return underlying.getSubscriptionMapper();
    }

    @Override
    public AnnotationMapper getAnnotationMapper() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public UidProvider getUidProvider() {
        return underlying.getUidProvider();
    }

    @Override
    public ModSeqProvider getModSeqProvider() {
        return underlying.getModSeqProvider();
    }

    @Override
    public MessageIdMapper getMessageIdMapper() {
        throw new NotImplementedException("Not implemented");
    }
}
