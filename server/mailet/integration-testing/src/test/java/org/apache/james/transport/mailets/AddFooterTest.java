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

public class AddFooterTest {/*

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int IMAP_PORT = 1143;
    private static final int SMTP_PORT = 1025;
    private static final String PASSWORD = "secret";

    private static final String JAMES_APACHE_ORG = "james.org";

    private static final String FROM = "fromUser@" + JAMES_APACHE_ORG;
    private static final String RECIPIENT = "touser@" + JAMES_APACHE_ORG;
    public static final String FOOTER = "MATCH_ME";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public IMAPMessageReader imapMessageReader = new IMAPMessageReader();

    private TemporaryJamesServer jamesServer;

    @Before
    public void setup() throws Exception {
        MailetContainer mailetContainer = MailetContainer.builder()
            .postmaster("postmaster@" + JAMES_APACHE_ORG)
            .addProcessor(CommonProcessors.root())
            .addProcessor(CommonProcessors.error())
            .addProcessor(ProcessorConfiguration.transport()
                .addMailet(MailetConfiguration.BCC_STRIPPER)
                .addMailet(MailetConfiguration.builder()
                    .matcher(All.class)
                    .mailet(AddFooter.class)
                    .addProperty("text", FOOTER))
                .addMailet(MailetConfiguration.builder()
                    .matcher(RecipientIsLocal.class)
                    .mailet(LocalDelivery.class)))
            .build();

        jamesServer = TemporaryJamesServer.builder()
            .withBase(MemoryJamesServerMain.SMTP_AND_IMAP_MODULE)
            .withMailetContainer(mailetContainer)
            .build(temporaryFolder);

        DataProbe dataProbe = jamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(JAMES_APACHE_ORG);
        dataProbe.addUser(FROM, PASSWORD);
        dataProbe.addUser(RECIPIENT, PASSWORD);
        jamesServer.getProbe(MailboxProbeImpl.class).createMailbox(MailboxConstants.USER_NAMESPACE, RECIPIENT, "INBOX");
    }

    @After
    public void tearDown() {
        jamesServer.shutdown();
    }

    @Test
    public void addFooterShouldAddFooterWhenSimpleMessage() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
            .addFrom(FROM)
            .addToRecipient(RECIPIENT)
            .setSubject("test")
            .setText("Any message we want");
        Mail mail = FakeMail.builder()
              .mimeMessage(message)
              .sender(new MailAddress(FROM))
              .recipient(new MailAddress(RECIPIENT))
              .build();

        try (SMTPMessageSender messageSender = SMTPMessageSender.noAuthentication(LOCALHOST_IP, SMTP_PORT, JAMES_APACHE_ORG);
                IMAPMessageReader imapMessageReader = new IMAPMessageReader(LOCALHOST_IP, IMAP_PORT)) {
            messageSender.sendMessage(mail);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(messageSender::messageHasBeenSent);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> imapMessageReader.hasAMessage(RECIPIENT, PASSWORD));
            String processedMessage = imapMessageReader.readFirstMessageInInbox(RECIPIENT, PASSWORD);
            assertThat(processedMessage).contains(FOOTER);
        }
    }

    @Test
    public void addFooterShouldAddFooterWhenMimePartMessage() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
            .addFrom(FROM)
            .addToRecipient(RECIPIENT)
            .setSubject("test")
            .setContent(MimeMessageBuilder.multipartBuilder()
                .withSubtype("mixed")
                .addBodies(
                    MimeMessageBuilder.bodyPartBuilder()
                        .type("text/plain")
                        .data(MimeMessageBuilder.multipartBuilder()
                            .addBody(MimeMessageBuilder.bodyPartBuilder()
                                .data("test"))),
                    MimeMessageBuilder.bodyPartBuilder()
                        .type("application/octet")
                        .data("attachment".getBytes(StandardCharsets.UTF_8))));
        Mail mail = FakeMail.builder()
              .mimeMessage(message)
              .sender(new MailAddress(FROM))
              .recipient(new MailAddress(RECIPIENT))
              .build();

        try (SMTPMessageSender messageSender = SMTPMessageSender.noAuthentication(LOCALHOST_IP, SMTP_PORT, JAMES_APACHE_ORG);
                IMAPMessageReader imapMessageReader = new IMAPMessageReader(LOCALHOST_IP, IMAP_PORT)) {
            messageSender.sendMessage(mail);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(messageSender::messageHasBeenSent);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> imapMessageReader.hasAMessage(RECIPIENT, PASSWORD));
            String processedMessage = imapMessageReader.readFirstMessageInInbox(RECIPIENT, PASSWORD);
            assertThat(processedMessage).contains(FOOTER);
        }
    }*/
}
