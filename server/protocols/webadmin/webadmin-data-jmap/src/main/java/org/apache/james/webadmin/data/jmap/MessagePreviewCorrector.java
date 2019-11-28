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

package org.apache.james.webadmin.data.jmap;

import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.jmap.api.preview.MessagePreviewStore;
import org.apache.james.jmap.api.preview.Preview;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroup;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.streams.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MessagePreviewCorrector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePreviewCorrector.class);

    static class Context {
        private final AtomicLong processedUserCount;
        private final AtomicLong processedMessageCount;
        private final AtomicLong failedUserCount;
        private final AtomicLong failedMessageCount;

        Context() {
            failedUserCount = new AtomicLong();
            processedMessageCount = new AtomicLong();
            processedUserCount = new AtomicLong();
            failedMessageCount = new AtomicLong();
        }

        long getProcessedUserCount() {
            return processedUserCount.get();
        }

        long getProcessedMessageCount() {
            return processedMessageCount.get();
        }

        long getFailedUserCount() {
            return failedUserCount.get();
        }

        long getFailedMessageCount() {
            return failedMessageCount.get();
        }

        boolean noFailure() {
            return failedMessageCount.get() == 0 && failedUserCount.get() == 0;
        }
    }

    private final UsersRepository usersRepository;
    private final MailboxManager mailboxManager;
    private final MessagePreviewStore messagePreviewStore;
    private final Preview.Factory previewFactory;

    @Inject
    MessagePreviewCorrector(UsersRepository usersRepository, MailboxManager mailboxManager,
                            MessagePreviewStore messagePreviewStore, Preview.Factory previewFactory) {
        this.usersRepository = usersRepository;
        this.mailboxManager = mailboxManager;
        this.messagePreviewStore = messagePreviewStore;
        this.previewFactory = previewFactory;
    }

    Mono<Void> correctAllPreviews(Context context) {
        try {
            return Iterators.toFlux(usersRepository.list())
                .concatMap(username -> correctAllPreviews(context, username))
                .then();
        } catch (UsersRepositoryException e) {
            return Mono.error(e);
        }
    }

    private Mono<Void> correctAllPreviews(Context context, Username username) {
        try {
            MailboxSession session = mailboxManager.createSystemSession(username);
            return Flux.fromIterable(mailboxManager.search(MailboxQuery.privateMailboxesBuilder(session).build(), session))
                .flatMap(mailboxMetadata -> Mono.fromCallable(() -> mailboxManager.getMailbox(mailboxMetadata.getId(), session)))
                .concatMap(Throwing.function(messageManager -> correctAllPreviews(context, messageManager, session)))
                .doOnComplete(context.processedUserCount::incrementAndGet)
                .onErrorContinue((error, o) -> {
                    LOGGER.error("JMAP preview re-computation aborted for {}", username, error);
                    context.failedUserCount.incrementAndGet();
                })
                .then();
        } catch (MailboxException e) {
            LOGGER.error("JMAP preview re-computation aborted for {} as we failed listing user mailboxes", username, e);
            context.failedUserCount.incrementAndGet();
            return Mono.empty();
        }
    }

    private Mono<Void> correctAllPreviews(Context context, MessageManager messageManager, MailboxSession session) throws MailboxException {
        return Iterators.toFlux(messageManager.getMessages(MessageRange.all(), FetchGroup.BODY_CONTENT, session))
            .map(Throwing.function(previewFactory::fromMessageResult))
            .flatMap(pair -> Mono.from(messagePreviewStore.store(pair.getKey(), pair.getValue()))
                .doOnSuccess(any -> context.processedMessageCount.incrementAndGet()))
            .onErrorContinue((error, triggeringValue) -> {
                LOGGER.error("JMAP preview re-computation aborted for {} - {}", session.getUser(), triggeringValue, error);
                context.failedMessageCount.incrementAndGet();
            })
            .then();
    }
}
