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

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.core.User;
import org.apache.james.jmap.exceptions.InvalidOriginMessageForMDNException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mdn.MDN;
import org.apache.james.mdn.MDNReport;
import org.apache.james.mdn.fields.Disposition;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.ParseException;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.james.util.OptionalUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

@JsonDeserialize(builder = JmapMDN.Builder.class)
public class JmapMDN {

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private MessageId messageId;
        private String subject;
        private String textBody;
        private String reportingUA;
        private MDNDisposition disposition;

        public Builder messageId(MessageId messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder textBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public Builder reportingUA(String reportingUA) {
            this.reportingUA = reportingUA;
            return this;
        }

        public Builder disposition(MDNDisposition disposition) {
            this.disposition = disposition;
            return this;
        }

        public JmapMDN build() {
            Preconditions.checkState(messageId != null, "'messageId' is mandatory");
            Preconditions.checkState(subject != null, "'subject' is mandatory");
            Preconditions.checkState(textBody != null, "'textBody' is mandatory");
            Preconditions.checkState(reportingUA != null, "'reportingUA' is mandatory");
            Preconditions.checkState(disposition != null, "'disposition' is mandatory");

            return new JmapMDN(messageId, subject, textBody, reportingUA, disposition);
        }

    }

    private final MessageId messageId;
    private final String subject;
    private final String textBody;
    private final String reportingUA;
    private final MDNDisposition disposition;

    @VisibleForTesting
    JmapMDN(MessageId messageId, String subject, String textBody, String reportingUA, MDNDisposition disposition) {
        this.messageId = messageId;
        this.subject = subject;
        this.textBody = textBody;
        this.reportingUA = reportingUA;
        this.disposition = disposition;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public String getSubject() {
        return subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getReportingUA() {
        return reportingUA;
    }

    public MDNDisposition getDisposition() {
        return disposition;
    }

    public Message generateMDNMessage(Message originalMessage, MailboxSession mailboxSession) throws ParseException, IOException, InvalidOriginMessageForMDNException {

        User user = User.fromUsername(mailboxSession.getUser().getUserName());

        return MDN.builder()
            .report(generateReport(originalMessage, mailboxSession))
            .humanReadableText(textBody)
            .build()
        .asMime4JMessageBuilder()
            .setTo(getSenderAddress(originalMessage))
            .setFrom(user.asString())
            .setSubject(subject)
            .setMessageId(MimeUtil.createUniqueMessageId(user.getDomainPart().orElse(null)))
            .build();
    }

    private String getSenderAddress(Message originalMessage) throws InvalidOriginMessageForMDNException {
        Optional<Mailbox> replyTo = Optional.ofNullable(originalMessage.getReplyTo())
            .map(AddressList::flatten)
            .flatMap(this::returnFirstAddress);
        Optional<Mailbox> sender = Optional.ofNullable(originalMessage.getSender());
        Optional<Mailbox> from = Optional.ofNullable(originalMessage.getFrom())
            .flatMap(this::returnFirstAddress);

        return OptionalUtils.or(replyTo, sender, from)
            .orElseThrow(() -> InvalidOriginMessageForMDNException.missingField("Sender"))
            .getAddress();
    }

    private Optional<Mailbox> returnFirstAddress(MailboxList mailboxList) {
        return mailboxList.stream().findFirst();
    }

    public MDNReport generateReport(Message originalMessage, MailboxSession mailboxSession) throws InvalidOriginMessageForMDNException {
        if (originalMessage.getMessageId() == null) {
            throw InvalidOriginMessageForMDNException.missingField("Message-ID");
        }
        return MDNReport.builder()
            .dispositionField(generateDisposition())
            .originalRecipientField(mailboxSession.getUser().getUserName())
            .originalMessageIdField(originalMessage.getMessageId())
            .finalRecipientField(mailboxSession.getUser().getUserName())
            .reportingUserAgentField(getReportingUA())
            .build();
    }

    private Disposition generateDisposition() {
        return Disposition.builder()
            .actionMode(disposition.getActionMode())
            .sendingMode(disposition.getSendingMode())
            .type(disposition.getType())
            .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof JmapMDN) {
            JmapMDN that = (JmapMDN) o;

            return Objects.equals(this.messageId, that.messageId)
                && Objects.equals(this.subject, that.subject)
                && Objects.equals(this.textBody, that.textBody)
                && Objects.equals(this.reportingUA, that.reportingUA)
                && Objects.equals(this.disposition, that.disposition);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(messageId, subject, textBody, reportingUA, disposition);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("messageId", messageId)
            .add("subject", subject)
            .add("textBody", textBody)
            .add("reportingUA", reportingUA)
            .add("mdnDisposition", disposition)
            .toString();
    }
}
