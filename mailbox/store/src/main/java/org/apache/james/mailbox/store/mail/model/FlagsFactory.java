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
package org.apache.james.mailbox.store.mail.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.Flags;

import com.google.common.base.Preconditions;

public class FlagsFactory {

    private static Flags asFlags(MailboxMessage mailboxMessage, String[] userFlags) {
        final Flags flags = new Flags();
        if (mailboxMessage.isAnswered()) {
            flags.add(Flags.Flag.ANSWERED);
        }
        if (mailboxMessage.isDeleted()) {
            flags.add(Flags.Flag.DELETED);
        }
        if (mailboxMessage.isDraft()) {
            flags.add(Flags.Flag.DRAFT);
        }
        if (mailboxMessage.isFlagged()) {
            flags.add(Flags.Flag.FLAGGED);
        }
        if (mailboxMessage.isRecent()) {
            flags.add(Flags.Flag.RECENT);
        }
        if (mailboxMessage.isSeen()) {
            flags.add(Flags.Flag.SEEN);
        }
        if (userFlags != null && userFlags.length > 0) {
            for (String userFlag : userFlags) {
                flags.add(userFlag);
            }
        }
        return flags;
    }

    public static Flags createFlags(MailboxMessage mailboxMessage, String[] userFlags) {
        return builder()
            .flags(asFlags(mailboxMessage, userFlags))
            .addUserFlags(userFlags)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ArrayList<String> userFlags;
        private Optional<Flags> flags;
        private Optional<FlagsFilter> flagsFilter;

        private Builder() {
            flagsFilter = Optional.empty();
            userFlags = new ArrayList<>();
            flags = Optional.empty();
        }

        public Builder flags(Flags flags) {
            this.flags = Optional.of(flags);
            return this;
        }

        public Builder filteringFlags(FlagsFilter filter) {
            flagsFilter = Optional.of(filter);
            return this;
        }

        public Builder addUserFlags(String... userFlags) {
            this.userFlags.addAll(Arrays.asList(userFlags));
            return this;
        }

        public Flags build() {
            Preconditions.checkState(flags.isPresent() || !userFlags.isEmpty());

            FlagsFilter flagsFilter = this.flagsFilter.orElse(FlagsFilter.noFilter());
            Flags flags = this.flags.orElse(new Flags());

            Stream<Flags.Flag> flagStream =
                toFlagStream(flags)
                    .filter(flagsFilter.getSystemFlagFilter());
            Stream<String> userFlagsStream =
                Stream
                    .concat(
                        toUserFlagSTream(flags),
                        userFlags.stream())
                    .distinct()
                    .filter(flagsFilter.getUserFlagFilter());

            final Flags result = new Flags();
            flagStream.forEach(result::add);
            userFlagsStream.forEach(result::add);
            return result;
        }

        private Stream<Flags.Flag> toFlagStream(Flags flags) {
            return Arrays.stream(flags.getSystemFlags());
        }

        private Stream<String> toUserFlagSTream(Flags flags) {
            return Arrays.stream(flags.getUserFlags());
        }

    }
}
