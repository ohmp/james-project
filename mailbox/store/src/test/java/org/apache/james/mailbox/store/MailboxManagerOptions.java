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

import java.util.Optional;

import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.NoReservedMailboxesMatcher;
import org.apache.james.mailbox.model.ReservedMailboxMatcher;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;

public class MailboxManagerOptions {

    public static class Builder {
        private Optional<Integer> limitAnnotationCount;
        private Optional<Integer>  limitAnnotationSize;
        private Optional<ReservedMailboxMatcher> reservedMailboxMatcher;
        private Optional<Authorizator> authorizator;
        private Optional<Authenticator> authenticator;
        private Optional<GroupMembershipResolver> groupMembershipResolver;
        private Optional<MessageParser> messageParser;

        private Builder() {
            limitAnnotationCount = Optional.empty();
            limitAnnotationSize = Optional.empty();
            reservedMailboxMatcher = Optional.empty();
            authorizator = Optional.empty();
            authenticator = Optional.empty();
            groupMembershipResolver = Optional.empty();
            messageParser = Optional.empty();
        }

        public Builder withAnnotationCountLimit(int annotationCountLimit) {
            this.limitAnnotationCount = Optional.of(annotationCountLimit);
            return this;
        }

        public Builder withAnnotationSizeLimit(int annotationSizeLimit) {
            this.limitAnnotationSize = Optional.of(annotationSizeLimit);
            return this;
        }

        public Builder withReservedMailboxMatcher(ReservedMailboxMatcher reservedMailboxMatcher) {
            this.reservedMailboxMatcher = Optional.of(reservedMailboxMatcher);
            return this;
        }

        public Builder withAuthorizator(Authorizator authorizator) {
            this.authorizator = Optional.of(authorizator);
            return this;
        }

        public Builder withAuthenticator(Authenticator authenticator) {
            this.authenticator = Optional.of(authenticator);
            return this;
        }

        public Builder withGroupMembershipResolver(GroupMembershipResolver groupMembershipResolver) {
            this.groupMembershipResolver = Optional.of(groupMembershipResolver);
            return this;
        }

        public Builder withMessageParser(MessageParser messageParser) {
            this.messageParser = Optional.of(messageParser);
            return this;
        }

        public MailboxManagerOptions build() {
            return new MailboxManagerOptions(
                limitAnnotationCount.orElse(MailboxConstants.DEFAULT_LIMIT_ANNOTATIONS_ON_MAILBOX),
                limitAnnotationSize.orElse(MailboxConstants.DEFAULT_LIMIT_ANNOTATION_SIZE),
                reservedMailboxMatcher.orElse(new NoReservedMailboxesMatcher()),
                authorizator.orElse(FakeAuthorizator.defaultReject()),
                authenticator.orElse(new FakeAuthenticator()),
                groupMembershipResolver.orElse(new SimpleGroupMembershipResolver()),
                messageParser.orElse(new MessageParser()));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final MailboxManagerOptions NONE = builder().build();

    private final int limitAnnotationCount;
    private final int limitAnnotationSize;
    private final ReservedMailboxMatcher reservedMailboxMatcher;
    private final Authorizator authorizator;
    private final Authenticator authenticator;
    private final GroupMembershipResolver groupMembershipResolver;
    private final MessageParser messageParser;

    private MailboxManagerOptions(int limitAnnotationCount, int limitAnnotationSize,
                    ReservedMailboxMatcher reservedMailboxMatcher, Authorizator authorizator,
                    Authenticator authenticator, GroupMembershipResolver groupMembershipResolver, MessageParser messageParser) {
        this.limitAnnotationCount = limitAnnotationCount;
        this.limitAnnotationSize = limitAnnotationSize;
        this.reservedMailboxMatcher = reservedMailboxMatcher;
        this.authorizator = authorizator;
        this.authenticator = authenticator;
        this.groupMembershipResolver = groupMembershipResolver;
        this.messageParser = messageParser;
    }

    public MessageParser getMessageParser() {
        return messageParser;
    }

    public int getLimitAnnotationCount() {
        return limitAnnotationCount;
    }

    public int getLimitAnnotationSize() {
        return limitAnnotationSize;
    }

    public ReservedMailboxMatcher getReservedMailboxMatcher() {
        return reservedMailboxMatcher;
    }

    public Authorizator getAuthorizator() {
        return authorizator;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public GroupMembershipResolver getGroupMembershipResolver() {
        return groupMembershipResolver;
    }
}
