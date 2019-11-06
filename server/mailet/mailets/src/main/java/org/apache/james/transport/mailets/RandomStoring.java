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

package org.apache.james.transport.mailets;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.core.User;
import org.apache.james.transport.mailets.delivery.MailStore;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.util.streams.Iterators;
import org.apache.mailet.Attribute;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.base.MailetUtil;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.MoreObjects;

import reactor.core.publisher.Mono;

/**
 * Process messages and randomly assign them to 4 to 8 mailboxes.
 */
public class RandomStoring extends GenericMailet {
    public static final String PER_USER_MAILBOX_COUNT = "perUserMailboxCount";
    private static final int MIN_NUMBER_OF_RECIPIENTS = 4;
    private static final int MAX_NUMBER_OF_RECIPIENTS = 8;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);
    public static final String PROVISIONNED_MAILBOX_PREFIX = "provisionnedMailbox";

    private final Mono<List<User>> cachedUsers;
    private final UsersRepository usersRepository;
    private final Supplier<Integer> randomRecipientsNumbers;
    private int perUserMailboxCount;

    @Inject
    public RandomStoring(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
        this.randomRecipientsNumbers = () -> ThreadLocalRandom.current().nextInt(MIN_NUMBER_OF_RECIPIENTS, MAX_NUMBER_OF_RECIPIENTS + 1);
        this.cachedUsers = Mono.fromCallable(this::listUsers).cache(CACHE_DURATION);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        Collection<ReroutingInfos> reroutingInfos = generateRandomMailboxes();
        Collection<MailAddress> mailAddresses = reroutingInfos
            .stream()
            .map(Throwing.function(reroutingInfo -> reroutingInfo.user.asMailAddress()))
            .collect(Guavate.toImmutableList());

        mail.setRecipients(mailAddresses);
        reroutingInfos.forEach(reroutingInfo ->
            mail.setAttribute(Attribute.convertToAttribute(MailStore.DELIVERY_PATH_PREFIX + reroutingInfo.getUser().asString(), reroutingInfo.getMailbox())));
    }

    @Override
    public String getMailetInfo() {
        return "Random Storing Mailet";
    }

    @Override
    public void init() throws MessagingException {
        perUserMailboxCount = MailetUtil.getInitParameterAsStrictlyPositiveInteger(getInitParameter(PER_USER_MAILBOX_COUNT), 10);
    }

    private Collection<ReroutingInfos> generateRandomMailboxes() {
        List<User> users = cachedUsers.block();

        // Replaces Collections.shuffle() which has a too poor statistical distribution
        return ThreadLocalRandom
            .current()
            .ints(0, users.size())
            .mapToObj(users::get)
            .distinct()
            .limit(randomRecipientsNumbers.get())
            .map(user -> new ReroutingInfos(chooseRandomMailboxName(), user))
            .collect(Guavate.toImmutableSet());
    }

    private String chooseRandomMailboxName() {
        int mailboxNumber = ThreadLocalRandom
            .current()
            .nextInt(0, perUserMailboxCount);

        return PROVISIONNED_MAILBOX_PREFIX + mailboxNumber;
    }

    private List<User> listUsers() throws UsersRepositoryException {
        return Iterators.toStream(usersRepository.list())
            .map(User::fromUsername)
            .collect(Guavate.toImmutableList());
    }

    private static class ReroutingInfos {
        private final String mailbox;
        private final User user;

        ReroutingInfos(String mailbox, User user) {
            this.mailbox = mailbox;
            this.user = user;
        }

        public String getMailbox() {
            return mailbox;
        }

        public User getUser() {
            return user;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ReroutingInfos) {
                ReroutingInfos that = (ReroutingInfos) o;

                return Objects.equals(this.mailbox, that.mailbox)
                    && Objects.equals(this.user, that.user);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mailbox, user);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("mailbox", mailbox)
                .add("user", user)
                .toString();
        }
    }
}

