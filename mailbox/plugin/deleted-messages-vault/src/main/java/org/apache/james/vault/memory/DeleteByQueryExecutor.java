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

package org.apache.james.vault.memory;

import org.apache.james.core.User;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.task.Task;
import org.apache.james.vault.DeletedMessageVault;
import org.apache.james.vault.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DeleteByQueryExecutor {
    static class Notifiers {
        private final Runnable userHandledNotifier;
        private final Runnable searchErrorNotifier;
        private final Runnable deletionErrorNotifier;
        private final Runnable permanentlyDeletedMessageNotifyer;

        Notifiers(Runnable userHandledNotifier, Runnable searchErrorNotifier, Runnable deletionErrorNotifier, Runnable permanentlyDeletedMessageNotifyer) {
            this.userHandledNotifier = userHandledNotifier;
            this.searchErrorNotifier = searchErrorNotifier;
            this.deletionErrorNotifier = deletionErrorNotifier;
            this.permanentlyDeletedMessageNotifyer = permanentlyDeletedMessageNotifyer;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteByQueryExecutor.class);

    private final DeletedMessageVault deletedMessageVault;

    public DeleteByQueryExecutor(DeletedMessageVault deletedMessageVault) {
        this.deletedMessageVault = deletedMessageVault;
    }

    public Task.Result deleteByQuery(Query query, Notifiers notifiers) {
        return Flux.from(deletedMessageVault.usersWithVault())
            .flatMap(user -> deleteByQueryForUser(query, user, notifiers))
            .reduce(Task::combine)
            .onErrorResume(e -> {
                LOGGER.error("Unexpected error encountered while deleting by query", e);
                return Mono.just(Task.Result.PARTIAL);
            })
            .blockOptional()
            .orElse(Task.Result.COMPLETED);
    }

    private Mono<Task.Result> deleteByQueryForUser(Query query, User user, Notifiers notifiers) {
        return Flux.from(deletedMessageVault.search(user, query))
            .flatMap(message -> deleteMessage(user, message.getMessageId(), notifiers))
            .onErrorResume(e -> {
                LOGGER.error("Error encountered while searching old mails in {} vault", user.asString(), e);
                notifiers.searchErrorNotifier.run();
                return Mono.just(Task.Result.PARTIAL);
            })
            .reduce(Task::combine)
            .map(result -> result.run(
                    () -> LOGGER.info("Retention applied for {} vault", user.asString()),
                    notifiers.userHandledNotifier::run));
    }

    private Mono<Task.Result> deleteMessage(User user, MessageId messageId, Notifiers notifiers) {
        return Mono.from(deletedMessageVault.delete(user, messageId))
            .then(Mono.fromRunnable(notifiers.permanentlyDeletedMessageNotifyer))
            .thenReturn(Task.Result.COMPLETED)
            .onErrorResume(e -> {
                LOGGER.error("Error encountered while deleting a mail in {} vault: {}", user.asString(), messageId.serialize(), e);
                notifiers.deletionErrorNotifier.run();
                return Mono.just(Task.Result.PARTIAL);
            });
    }
}
