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

package org.apache.james.jmap.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.mail.Flags;

import org.apache.james.mailbox.FlagsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Keywords {

    public static final Keywords DEFAULT_VALUE = new Keywords(ImmutableSet.of());
    private static final Logger LOGGER = LoggerFactory.getLogger(Keywords.class);

    private FlagsBuilder combiner(FlagsBuilder firstBuilder, FlagsBuilder secondBuilder) {
        return firstBuilder.add(secondBuilder.build());
    }

    private FlagsBuilder accumulator(FlagsBuilder accumulator, Keyword keyword) {
        return accumulator.add(keyword.asFlags());
    }

    public static class KeywordsFactory {
        @FunctionalInterface
        interface KeywordsValidator {
            KeywordsValidator THROW_ON_IMAP_NON_EXPOSED_KEYWORDS = keywords -> Preconditions.checkArgument(
                keywords.stream().allMatch(Keyword::isExposedImapKeyword),
                "Does not allow to update 'Deleted' or 'Recent' flag");

            KeywordsValidator IGNORE_NON_EXPOSED_IMAP_KEYWORDS = keywords -> { };

            void validate(Set<Keyword> keywords);
        }

        @FunctionalInterface
        interface KeywordFilter extends Predicate<Keyword> {
            KeywordFilter FILTER_IMAP_NON_EXPOSED_KEYWORDS = Keyword::isExposedImapKeyword;
            KeywordFilter KEEP_ALL = keyword -> true;
        }

        private final KeywordsValidator validator;
        private final Predicate<Keyword> filter;

        public KeywordsFactory(KeywordsValidator validator, Predicate<Keyword> filter) {
            this.validator = validator;
            this.filter = filter;
        }

        public Keywords fromSet(Set<Keyword> setKeywords) {
            validator.validate(setKeywords);

            return new Keywords(setKeywords.stream()
                    .filter(filter)
                    .collect(Guavate.toImmutableSet()));
        }

        public Keywords from(Keyword... keywords) {
            return fromSet(Arrays.stream(keywords)
                    .collect(Guavate.toImmutableSet()));
        }

        public Keywords fromList(List<String> keywords) {
            return fromSet(keywords.stream()
                    .map(Keyword::of)
                    .collect(Guavate.toImmutableSet()));
        }

        @VisibleForTesting
        Keywords fromMap(Map<String, Boolean> mapKeywords) {
            Preconditions.checkArgument(mapKeywords.values()
                .stream()
                .allMatch(keywordValue -> keywordValue), "Keyword must be true");
            Set<Keyword> setKeywords = mapKeywords.keySet()
                .stream()
                .map(Keyword::of)
                .collect(Guavate.toImmutableSet());

            return fromSet(setKeywords);
        }

        public Keywords fromFlags(Flags flags) {
            return fromSet(Stream.concat(
                        Stream.of(flags.getUserFlags())
                            .flatMap(this::asKeyword),
                        Stream.of(flags.getSystemFlags())
                            .map(Keyword::fromFlag))
                    .collect(Guavate.toImmutableSet()));
        }

        private Stream<Keyword> asKeyword(String flagName) {
            try {
                return Stream.of(Keyword.of(flagName));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Fail to parse {} flag", flagName, e);
                return Stream.of();
            }
        }
    }

    public static KeywordsFactory strictFactory() {
        return new KeywordsFactory(KeywordsFactory.KeywordsValidator.THROW_ON_IMAP_NON_EXPOSED_KEYWORDS,
            KeywordsFactory.KeywordFilter.KEEP_ALL);
    }

    public static KeywordsFactory lenientFactory() {
        return new KeywordsFactory(KeywordsFactory.KeywordsValidator.IGNORE_NON_EXPOSED_IMAP_KEYWORDS,
            KeywordsFactory.KeywordFilter.FILTER_IMAP_NON_EXPOSED_KEYWORDS);
    }

    private final ImmutableSet<Keyword> keywords;

    private Keywords(ImmutableSet<Keyword> keywords) {
        this.keywords = keywords;
    }

    public Flags asFlags() {
        return keywords.stream()
            .reduce(FlagsBuilder.builder(), this::accumulator, this::combiner)
            .build();
    }

    public Flags asFlagsWithRecentAndDeletedFrom(Flags originFlags) {
        Flags flags = asFlags();
        if (originFlags.contains(Flags.Flag.DELETED)) {
            flags.add(Flags.Flag.DELETED);
        }
        if (originFlags.contains(Flags.Flag.RECENT)) {
            flags.add(Flags.Flag.RECENT);
        }

        return flags;
    }

    public ImmutableMap<String, Boolean> asMap() {
        return keywords.stream()
            .collect(Guavate.toImmutableMap(Keyword::getFlagName, keyword -> Keyword.FLAG_VALUE));
    }

    public ImmutableSet<Keyword> getKeywords() {
        return keywords;
    }

    public boolean contains(Keyword keyword) {
        return keywords.contains(keyword);
    }

    @Override
    public final boolean equals(Object other) {
        if (other instanceof Keywords) {
            Keywords otherKeyword = (Keywords) other;
            return Objects.equal(keywords, otherKeyword.keywords);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(keywords);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("keywords", keywords)
            .toString();
    }

}
