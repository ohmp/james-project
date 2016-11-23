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

public class MimeMessageReaderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MessageParser messageParser;

    @Before
    public void setUp() {
        messageParser = mock(MessageParser.class);
    }

    @Test
    public void initShouldThrowWithNullStream() throws Exception {
        expectedException.expect(NullPointerException.class);
        new MimeMessageReader(messageParser, null);
    }

    @Test
    public void readShouldCopyContent() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(IOUtils.toString(mimeMessageReader.read().getContentIn(), Charsets.UTF_8))
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResourceAsStream("eml/mail.eml"), Charsets.UTF_8));
    }

    @Test
    public void readShouldCalculateSize() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getSize())
            .isEqualTo(5250);
    }

    @Test
    public void readShouldCalculateBodyStartOctet() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getBodyStartOctet())
            .isEqualTo(4785);
    }

    @Test
    public void readShouldCalculateLineCountOfTextParts() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getTextualLineCount())
            .isEqualTo(10);
    }

    @Test
    public void readShouldRetrieveCharset() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getCharset())
            .isEqualTo("UTF-8");
    }

    @Test
    public void readShouldRetrieveTransferEncoding() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getContentTransferEncoding())
            .isEqualTo("7bit");
    }

    @Test
    public void readShouldRetrieveMediaType() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getMediaType())
            .isEqualTo("text");
    }

    @Test
    public void readShouldRetrieveSubtype() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getSubType())
            .isEqualTo("alternative");
    }

    @Test
    public void readShouldRetrieveBoundary() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getBoundary())
            .isEqualTo("--==_mimepart_556fffe8c7e84_7ed0e0fe20445637");
    }

    @Test
    public void readShouldReturnNullBoundaryWhenSimpleMail() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/mail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getBoundary())
            .isNull();
    }

    @Test
    public void readShouldReturnEmptyLanguageWhenNoneSpecified() throws Exception {
        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read().getPropertyBuilder().getContentLanguage())
            .isEmpty();
    }

    @Test
    public void readShouldReturnAttachments() throws Exception {
        ImmutableList<MessageAttachment> attachments = ImmutableList.of();
        when(messageParser.retrieveAttachments(any(InputStream.class))).thenReturn(attachments);

        MimeMessageReader mimeMessageReader = new MimeMessageReader(messageParser, ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"));

        assertThat(mimeMessageReader.read().getAttachments()).isEmpty();
    }

}
