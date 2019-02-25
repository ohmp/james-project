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

import org.apache.james.core.User;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailrepository.api.MailKey;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.util.streams.Iterators;
import org.reactivestreams.Publisher;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class MailRepositoryBasedDeletedMessageVault implements DeletedMessageVault {
    public static class Configuration {
        private final String urlPrefix;

        public Configuration(String urlPrefix) {
            this.urlPrefix = urlPrefix;
        }
    }

    private final MailRepositoryStore mailRepositoryStore;
    private final Configuration configuration;
    private final MailAdapter mailAdapter;

    public MailRepositoryBasedDeletedMessageVault(MailRepositoryStore mailRepositoryStore, Configuration configuration, MailAdapter mailAdapter) {
        this.mailRepositoryStore = mailRepositoryStore;
        this.configuration = configuration;
        this.mailAdapter = mailAdapter;
    }

    Mono<MailRepository> repositoryForUser(User user) {
        MailRepositoryUrl mailRepositoryUrl = MailRepositoryUrl.from(configuration.urlPrefix + user.asString());
        return Mono.fromCallable(() -> mailRepositoryStore.select(mailRepositoryUrl));
    }

    @Override
    public Publisher<Void> append(User user, DeletedMessage deletedMessage) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(deletedMessage);

        return repositoryForUser(user)
            .flatMap(mailRepository -> Mono.fromCallable(() -> mailRepository.store(mailAdapter.toMail(deletedMessage))))
            .then()
            .publishOn(Schedulers.elastic());
    }

    @Override
    public Publisher<Void> delete(User user, MessageId messageId) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(messageId);

        MailKey mailKey = new MailKey(messageId.serialize());

        return repositoryForUser(user)
            .publishOn(Schedulers.elastic())
            .flatMap(repository -> Mono.fromRunnable(
                Throwing.runnable(() -> repository.remove(mailKey))));
    }

    @Override
    public Publisher<DeletedMessage> search(User user, Query query) {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(query);
        Preconditions.checkArgument(query.getCriteria().isEmpty(), "Search is not supported yet...");

        Mono<MailRepository> mailRepositoryMono = repositoryForUser(user).cache();

        return mailRepositoryMono
            .flatMapMany(Throwing.function(repository -> Flux.fromStream(Iterators.toStream(repository.list()))))
            .flatMap(mailKey -> mailRepositoryMono.flatMap(repository -> Mono.fromCallable(() -> repository.retrieve(mailKey))))
            .map(mailAdapter::fromMail)
            .publishOn(Schedulers.elastic());
    }
}
