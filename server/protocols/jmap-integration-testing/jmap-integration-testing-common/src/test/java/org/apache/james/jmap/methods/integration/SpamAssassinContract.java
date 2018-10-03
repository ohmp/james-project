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

package org.apache.james.jmap.methods.integration;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.jmap.HttpJmapAuthentication.authenticateJamesUser;
import static org.apache.james.jmap.JmapCommonRequests.getInboxId;
import static org.apache.james.jmap.JmapCommonRequests.getOutboxId;
import static org.apache.james.jmap.JmapCommonRequests.getSpamId;
import static org.apache.james.jmap.JmapCommonRequests.getTrashId;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.LOCALHOST_IP;
import static org.apache.james.jmap.TestingConstants.NAME;
import static org.apache.james.jmap.TestingConstants.calmlyAwait;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.JmapGuiceProbe;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;

public interface SpamAssassinContract {
    String BOBS_DOMAIN = "spamer.com";
    String BOB = "bob@" + BOBS_DOMAIN;
    String BOB_PASSWORD = "bobPassword";
    String RECIPIENTS_DOMAIN = "angels.org";
    String ALICE = "alice@" + RECIPIENTS_DOMAIN;
    String ALICE_PASSWORD = "alicePassword";
    String PAUL = "paul@" + RECIPIENTS_DOMAIN;
    String PAUL_PASSWORD = "paulPassword";

    void train(String username) throws Exception;

    @BeforeEach
    default void setup(GuiceJamesServer james) throws Throwable {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .setPort(james.getProbe(JmapGuiceProbe.class).getJmapPort())
                .build();
        RestAssured.defaultParser = Parser.JSON;

        james.getProbe(DataProbeImpl.class)
            .fluent()
            .addDomain(BOBS_DOMAIN)
            .addDomain(RECIPIENTS_DOMAIN)
            .addUser(BOB, BOB_PASSWORD)
            .addUser(ALICE, ALICE_PASSWORD)
            .addUser(PAUL, PAUL_PASSWORD);
    }

    default AccessToken accessTokenFor(GuiceJamesServer james, String user, String password) {
        return authenticateJamesUser(baseUri(james), user, password);
    }

    @Test
    default void spamShouldBeDeliveredInSpamMailboxWhenSameMessageHasAlreadyBeenMovedToSpam(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 2 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 2));
    }

    @Test
    default void imapCopiesToSpamMailboxShouldBeConsideredAsSpam(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST_IP, james.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(ALICE, ALICE_PASSWORD)
                .select(IMAPMessageReader.INBOX);

            imapMessageReader.copyFirstMessage("Spam");
        }
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 2 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 2));
    }

    @Test
    default void imapMovesToSpamMailboxShouldBeConsideredAsSpam(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST_IP, james.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(ALICE, ALICE_PASSWORD)
                .select(IMAPMessageReader.INBOX);

            imapMessageReader.moveFirstMessage("Spam");
        }
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 2 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 2));
    }

    @Test
    default void spamAssassinShouldForgetMessagesMovedOutOfSpamFolderUsingJMAP(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
            .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Alice is moving this message out of Spam -> forgetting in SpamAssassin
        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getInboxId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice INBOX mailbox (she now must have 2 messages in her Inbox mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 2));
    }

    @Test
    default void movingAMailToTrashShouldNotImpactSpamassassinLearning(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
            .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Alice is moving this message to trash
        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getTrashId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getTrashId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 1 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));
    }

    @Test
    default void spamAssassinShouldForgetMessagesMovedOutOfSpamFolderUsingIMAP(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
            .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Alice is moving this message out of Spam -> forgetting in SpamAssassin
        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST_IP, james.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(ALICE, ALICE_PASSWORD)
                .select("Spam");

            imapMessageReader.moveFirstMessage(IMAPMessageReader.INBOX);
        }
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice INBOX mailbox (she now must have 2 messages in her Inbox mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 2));
    }

    @Test
    default void expungingSpamMessageShouldNotImpactSpamAssassinState(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
            .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Alice is deleting this message
        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST_IP, james.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(ALICE, ALICE_PASSWORD)
                .select("Spam");

            imapMessageReader.setFlagsForAllMessagesInMailbox("\\Deleted");
            imapMessageReader.expunge();
        }
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 0));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice SPAM mailbox (she now must have 1 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));
    }

    @Test
    default void deletingSpamMessageShouldNotImpactSpamAssassinState(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);

        // Bob is sending a message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
            .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Alice is deleting this message
        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"destroy\": [\"%s\"] }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".destroyed", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 0));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice SPAM mailbox (she now must have 1 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));
    }

    default String setMessageCreate(AccessToken accessToken) {
        return "[" +
            "  [" +
            "    \"setMessages\"," +
            "    {" +
            "      \"create\": { \"creationId1337\" : {" +
            "        \"from\": { \"email\": \"" + BOB + "\"}," +
            "        \"to\": [{ \"name\": \"recipient\", \"email\": \"" + ALICE + "\"}]," +
            "        \"subject\": \"Happy News\"," +
            "        \"textBody\": \"This is a SPAM!!!\r\n\r\n\"," +
            "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";
    }

    @Test
    default void spamShouldBeDeliveredInSpamMailboxOrInboxWhenMultipleRecipientsConfigurations(GuiceJamesServer james) throws Exception {
        train(ALICE);
        AccessToken aliceAccessToken = accessTokenFor(james, ALICE, ALICE_PASSWORD);
        AccessToken bobAccessToken = accessTokenFor(james, BOB, BOB_PASSWORD);
        AccessToken paulAccessToken = accessTokenFor(james, PAUL, PAUL_PASSWORD);

        // Bob is sending a message to Alice & Paul
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreateToMultipleRecipients(bobAccessToken))
        .when()
            .post("/jmap");
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 1));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(paulAccessToken, getInboxId(paulAccessToken), 1));

        // Alice is moving this message to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(1))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> given()
                .header("Authorization", aliceAccessToken.serialize())
                .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messagesSet"))
                .body(ARGUMENTS + ".updated", hasSize(1)));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 1));

        // Bob is sending again the same message to Alice & Paul
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreateToMultipleRecipients(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 2 messages in her Spam mailbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), 2));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), 0));
        // This message is delivered in Paul Inbox (he now must have 2 messages in his Inbox)
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(paulAccessToken, getInboxId(paulAccessToken), 2));
        calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> areMessagesFoundInMailbox(paulAccessToken, getSpamId(paulAccessToken), 0));
    }

    default String setMessageCreateToMultipleRecipients(AccessToken accessToken) {
        return "[" +
            "  [" +
            "    \"setMessages\"," +
            "    {" +
            "      \"create\": { \"creationId1337\" : {" +
            "        \"from\": { \"email\": \"" + BOB + "\"}," +
            "        \"to\": [{ \"name\": \"alice\", \"email\": \"" + ALICE + "\"}, " +
            "                 { \"name\": \"paul\", \"email\": \"" + PAUL + "\"}]," +
            "        \"subject\": \"Happy News\"," +
            "        \"textBody\": \"This is a SPAM!!!\r\n\r\n\"," +
            "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";
    }

    default boolean areMessagesFoundInMailbox(AccessToken accessToken, String mailboxId, int expectedNumberOfMessages) {
        try {
            with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + mailboxId + "\"]}}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messageList"))
                .body(ARGUMENTS + ".messageIds", hasSize(expectedNumberOfMessages));
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }
}
