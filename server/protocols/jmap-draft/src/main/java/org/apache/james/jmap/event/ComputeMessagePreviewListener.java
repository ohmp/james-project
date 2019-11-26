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

package org.apache.james.jmap.event;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.apache.james.jmap.api.preview.MessagePreviewStore;
import org.apache.james.jmap.api.preview.Preview;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroup;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.mailbox.store.SessionProvider;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.MimeConfig;
import org.apache.james.util.html.HtmlTextExtractor;
import org.apache.james.util.mime.MessageContentExtractor;
import org.apache.james.util.mime.MessageContentExtractor.MessageContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class ComputeMessagePreviewListener implements MailboxListener.GroupMailboxListener {
    public static class ComputeMessagePreviewListenerGroup extends Group {

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeMessagePreviewListener.class);
    private static final Group GROUP = new ComputeMessagePreviewListenerGroup();

    private final MessageIdManager messageIdManager;
    private final MessagePreviewStore messagePreviewStore;
    private final SessionProvider sessionProvider;
    private final MessageContentExtractor messageContentExtractor;
    private final HtmlTextExtractor htmlTextExtractor;

    @Inject
    public ComputeMessagePreviewListener(SessionProvider sessionProvider, MessageIdManager messageIdManager,
                                         MessagePreviewStore messagePreviewStore, MessageContentExtractor messageContentExtractor,
                                         HtmlTextExtractor htmlExtractor) {
        this.sessionProvider = sessionProvider;
        this.messageIdManager = messageIdManager;
        this.messagePreviewStore = messagePreviewStore;
        this.messageContentExtractor = messageContentExtractor;
        this.htmlTextExtractor = htmlExtractor;
    }

    @Override
    public Group getDefaultGroup() {
        return GROUP;
    }

    @Override
    public void event(Event event) throws MailboxException {
        if (event instanceof Added) {
            MailboxSession session = sessionProvider.createSystemSession(event.getUsername());
            handleAddedEvent((Added) event, session);
        }
    }

    private void handleAddedEvent(Added addedEvent, MailboxSession session) throws MailboxException {
        List<MessageId> messageIds = addedEvent.getUids()
            .stream()
            .map(uid -> addedEvent.getMetaData(uid).getMessageId())
            .distinct()
            .collect(Guavate.toImmutableList());

        preComputePreview(messageIds, session);
    }

    private void preComputePreview(List<MessageId> messageIds, MailboxSession session) throws MailboxException {
        Flux.fromIterable(messageIdManager.getMessages(messageIds, FetchGroup.BODY_CONTENT, session))
            .map(Throwing.function(this::computePreview))
            .onErrorContinue((throwable, message) ->
                LOGGER.error("Error while computing preview for message {}", message, throwable))
            .flatMap(message -> messagePreviewStore.store(message.getT1(), message.getT2()))
            .blockLast();
    }

    private Tuple2<MessageId, Preview> computePreview(MessageResult messageResult) throws MailboxException, IOException {
        Message mimeMessage = parse(messageResult);
        MessageContent messageContent = messageContentExtractor.extract(mimeMessage);
        Preview preview = messageContent.mainTextContent(htmlTextExtractor)
            .map(Preview::compute)
            .orElse(Preview.from(""));

        return Tuples.of(messageResult.getMessageId(), preview);
    }

    private Message parse(MessageResult message) throws MailboxException, IOException {
        return Message.Builder.of()
            .use(MimeConfig.PERMISSIVE)
            .parse(message.getFullContent().getInputStream())
            .build();
    }
}
