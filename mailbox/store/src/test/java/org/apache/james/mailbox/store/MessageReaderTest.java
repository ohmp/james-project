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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.omg.CORBA.portable.InputStream;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class MessageReaderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MessageReader messageReader;
    private MessageParser mock;

    @Before
    public void setUp() {
        mock = mock(MessageParser.class);
        messageReader = new MessageReader(mock);
    }

    @Test
    public void initShouldThrowWithNullStream() throws Exception {
        expectedException.expect(NullPointerException.class);
        messageReader.init(null);
    }

    @Test
    public void readShouldCopyContent() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(IOUtils.toString(messageReader.read().getContentIn(), Charsets.UTF_8))
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResourceAsStream("eml/mail.eml"), Charsets.UTF_8));
    }

    @Test
    public void readShouldCalculateSize() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getSize())
            .isEqualTo(5250);
    }

    @Test
    public void readShouldCalculateBodyStartOctet() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getBodyStartOctet())
            .isEqualTo(4785);
    }

    @Test
    public void readShouldCalculateLineCountOfTextParts() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getTextualLineCount())
            .isEqualTo(10);
    }

    @Test
    public void readShouldRetrieveCharset() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getCharset())
            .isEqualTo("UTF-8");
    }

    @Test
    public void readShouldRetrieveTransferEncoding() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getContentTransferEncoding())
            .isEqualTo("7bit");
    }

    @Test
    public void readShouldRetrieveMediaType() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getMediaType())
            .isEqualTo("text");
    }

    @Test
    public void readShouldRetrieveSubtype() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getSubType())
            .isEqualTo("alternative");
    }

    @Test
    public void readShouldRetrieveBoundary() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getBoundary())
            .isEqualTo("--==_mimepart_556fffe8c7e84_7ed0e0fe20445637");
    }

    @Test
    public void readShouldReturnNullBoundaryWhenSimpleMail() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getBoundary())
            .isNull();
    }

    @Test
    public void readShouldReturnEmptyLanguageWhenNoneSpecified() throws Exception {
        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(messageReader.read().getPropertyBuilder().getContentLanguage())
            .isEmpty();
    }

    @Test
    public void readShouldReturnAttachments() throws Exception {
        ImmutableList<MessageAttachment> attachments = ImmutableList.of();
        when(mock.retrieveAttachments(any(InputStream.class))).thenReturn(attachments);

        messageReader.init(ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(messageReader.read().getAttachments()).isEmpty();
    }

}
