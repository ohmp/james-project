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

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.james.util.StreamUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * <p>Collects the sender and the recipients of a message and store them as JSON in a specified message attribute.</p>
 * <p>Here is the JSON format:</p>
 * <pre><code>
 * {
 *   "userEmail" : "sender@james.org", 
 *   "emails" : [ "to@james.org", "cc@james.org" ]
 * }
 * </code></pre>
 * 
 * <p>Sample configuration:</p>
 *
 * <pre><code>
 * &lt;mailet match="All" class="ContactExtractor"&gt;
 *   &lt;attribute&gt;ExtractedContacts&lt;/attribute&gt;
 * &lt;/mailet&gt;
 * </code></pre>
 */
public class ContactExtractor extends GenericMailet implements Mailet {

    public interface Configuration {
        String ATTRIBUTE = "attribute";
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactExtractor.class);

    @VisibleForTesting ObjectMapper objectMapper;
    private String extractAttributeTo;

    @Override
    public void init() throws MessagingException {
        extractAttributeTo = getInitParameterAsOptional(Configuration.ATTRIBUTE)
                .orElseThrow(() -> new MailetException("No value for " + Configuration.ATTRIBUTE + " parameter was provided."));

        objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    }

    @Override
    public String getMailetInfo() {
        return "ContactExtractor Mailet" ;
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        try {
            Optional<String> payload = extractContacts(mail);
            LOGGER.debug("payload : {}", payload);
            payload.ifPresent(x -> mail.setAttribute(extractAttributeTo, x));
        } catch (Exception e) {
            LOGGER.error("Error while extracting contacts", e);
        }
    }

    @VisibleForTesting
    Optional<String> extractContacts(Mail mail) throws MessagingException, IOException {
        ImmutableList<String> allRecipients = getAllRecipients(mail.getMessage());

        return Optional.of(mail.getSender())
            .map(MailAddress::asString)
            .filter(sender -> !allRecipients.isEmpty())
            .map(sender -> new ExtractedContacts(sender, allRecipients))
            .map(Throwing.function(extractedContacts -> objectMapper.writeValueAsString(extractedContacts)));
    }

    private ImmutableList<String> getAllRecipients(MimeMessage mimeMessage) throws MessagingException {
        return StreamUtils.flatten(
            Stream.of(getRecipients(mimeMessage, Message.RecipientType.TO.toString()),
                getRecipients(mimeMessage, Message.RecipientType.CC.toString()),
                getRecipients(mimeMessage, Message.RecipientType.BCC.toString())))
            .collect(Guavate.toImmutableList());
    }

    private Stream<String> getRecipients(MimeMessage mimeMessage, String headerName) throws MessagingException {
        boolean strict = false;
        FunctionChainer<String, InternetAddress[]> function =
            Throwing.function(string -> InternetAddress.parseHeader(string, strict));
        return Optional.ofNullable(mimeMessage.getHeader(headerName, ","))
            .map(function.sneakyThrow())
            .map(Arrays::stream)
            .orElse(Stream.empty())
            .map(Address::toString)
            .map(MimeUtil::unscrambleHeaderValue);
    }

    public static class ExtractedContacts {
        private final String userEmail;
        private final ImmutableList<String> emails;

        public ExtractedContacts(String userEmail, ImmutableList<String> emails) {
            this.emails = emails;
            this.userEmail = userEmail;
        }

        public ImmutableList<String> getEmails() {
            return emails;
        }

        public String getUserEmail() {
            return userEmail;
        }
    }
}
