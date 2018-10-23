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

package org.apache.james.jmap.mailet.filter;

import static org.apache.mailet.base.RFC2822Headers.FROM;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.Message;

import org.apache.james.javax.AddressHelper;
import org.apache.james.jmap.api.filtering.Rule;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.james.util.StreamUtils;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.functions.ThrowingFunction;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

public interface HeaderExtractor extends ThrowingFunction<Mail, Stream<String>> {
    Logger LOGGER = LoggerFactory.getLogger(HeaderExtractor.class);

    HeaderExtractor SUBJECT_EXTRACTOR = mail ->
        StreamUtils.ofNullables(mail.getMessage().getSubject());
    HeaderExtractor CC_EXTRACTOR = recipientExtractor(Message.RecipientType.CC);
    HeaderExtractor TO_EXTRACTOR = recipientExtractor(Message.RecipientType.TO);
    HeaderExtractor RECIPIENT_EXTRACTOR = and(TO_EXTRACTOR, CC_EXTRACTOR);
    HeaderExtractor FROM_EXTRACTOR = addressExtractor(mail -> mail.getMessage().getFrom(), FROM);

    Map<Rule.Condition.Field, HeaderExtractor> HEADER_EXTRACTOR_REGISTRY = ImmutableMap.<Rule.Condition.Field, HeaderExtractor>builder()
        .put(Rule.Condition.Field.SUBJECT, SUBJECT_EXTRACTOR)
        .put(Rule.Condition.Field.RECIPIENT, RECIPIENT_EXTRACTOR)
        .put(Rule.Condition.Field.FROM, FROM_EXTRACTOR)
        .put(Rule.Condition.Field.CC, CC_EXTRACTOR)
        .put(Rule.Condition.Field.TO, TO_EXTRACTOR)
        .build();

    static HeaderExtractor and(HeaderExtractor headerExtractor1, HeaderExtractor headerExtractor2) {
        return (Mail mail) -> StreamUtils.flatten(headerExtractor1.apply(mail), headerExtractor2.apply(mail));
    }

    static HeaderExtractor recipientExtractor(Message.RecipientType type) {
        ThrowingFunction<Mail, Address[]> addressGetter = mail -> mail.getMessage().getRecipients(type);
        String fallbackHeaderName = type.toString();

        return addressExtractor(addressGetter, fallbackHeaderName);
    }

    static HeaderExtractor addressExtractor(ThrowingFunction<Mail, Address[]> addressGetter, String fallbackHeaderName) {
        return mail -> {
            try {
                return toContent(addressGetter.apply(mail));
            } catch (Exception e) {
                LOGGER.info("Failed parsing header. Falling back to unparsed header value matching", e);
                return Stream.of(mail.getMessage().getHeader(fallbackHeaderName))
                    .map(MimeUtil::unscrambleHeaderValue)
                    .flatMap(s -> Splitter.on(',').trimResults().splitToList(s).stream());
            }
        };
    }

    static Stream<String> toContent(Address[] addresses) {
        return Optional.ofNullable(addresses)
            .map(AddressHelper::asStringStream)
            .orElse(Stream.empty());
    }

    static Optional<HeaderExtractor> asHeaderExtractor(Rule.Condition.Field field) {
        return Optional.ofNullable(
            HeaderExtractor.HEADER_EXTRACTOR_REGISTRY.get(field));
    }
}
