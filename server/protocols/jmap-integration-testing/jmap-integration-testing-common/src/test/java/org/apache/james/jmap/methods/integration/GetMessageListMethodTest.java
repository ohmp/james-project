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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.utils.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.utils.MailboxRetriever;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

public abstract class GetMessageListMethodTest {
    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";

    protected abstract GuiceJamesServer createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private String username;
    private String domain;
    private GuiceJamesServer jmapServer;

    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        RestAssured.port = jmapServer.getJmapPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));

        this.domain = "domain.tld";
        this.username = "username@" + domain;
        String password = "password";
        jmapServer.serverProbe().addDomain(domain);
        jmapServer.serverProbe().addUser(username, password);
        this.accessToken = JmapAuthentication.authenticateJamesUser(username, password);
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }

    @Test
    public void getMessagesShouldBehaveAsIfMailboxDidNotExistWhenUsedOnAnOtherUserMailbox() throws Exception {
        String attacker = "attacker@" + domain;
        String password = "password";
        jmapServer.serverProbe().addUser(attacker, password);
        AccessToken attackerAccessToken =  JmapAuthentication.authenticateJamesUser(attacker, password);

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "INBOX");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "INBOX"),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String victimInboxId = MailboxRetriever.getInboxId(accessToken);

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", attackerAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + victimInboxId + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", empty());
    }
    
    @Test
    public void getMessageListShouldErrorInvalidArgumentsWhenRequestIsInvalid() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\": true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("Can not instantiate value of type [simple type, class org.apache.james.jmap.model.FilterCondition$Builder] from Boolean value (true); no single-boolean/Boolean-arg constructor/factory method\n" + 
                    " at [Source: {\"filter\":true}; line: 1, column: 2] (through reference chain: org.apache.james.jmap.model.Builder[\"filter\"])"));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenSingleMailboxNoParameters() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenMultipleMailboxesAndNoParameters() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox2"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox2|1"));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesOfCurrentUserOnlyWhenMultipleMailboxesAndNoParameters() throws Exception {
        String otherUser = "other@" + domain;
        String password = "password";
        jmapServer.serverProbe().addUser(otherUser, password);

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox2"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, otherUser, "mailbox");
        jmapServer.serverProbe().appendMessage(otherUser, new MailboxPath(MailboxConstants.USER_NAMESPACE, otherUser, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox2|1"));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenInMailboxesFilterMatches() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String mailboxId = jmapServer.serverProbe().getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox").getMailboxId().serialize();
        
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + mailboxId + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|1"));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenMultipleInMailboxesFilterMatches() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        await();

        List<String> mailboxIds =
                with()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
                .post("/jmap")
                .path("[0][1].list.id");
        
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"%s\", \"%s\"]}}, \"#0\"]]", mailboxIds.get(0), mailboxIds.get(1)))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|1"));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenInMailboxesFilterDoesntMatches() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"mailbox2\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(ARGUMENTS + ".messageIds", empty());
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateDefault() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateAsc() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date asc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|2", "username@domain.tld|mailbox|1"));
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateDesc() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date desc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldWorkWhenCollapseThreadIsFalse() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"collapseThreads\":false}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"));
    }
    
    @Test
    public void getMessageListShouldWorkWhenCollapseThreadIsTrue() {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"collapseThreads\":true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"));
    }
    
    @Test
    public void getMessageListShouldReturnAllMessagesWhenPositionIsNotGiven() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldReturnSkipMessagesWhenPositionIsGiven() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"position\":1}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenLimitIsNotGiven() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2"));
    }

    @Test
    public void getMessageListShouldReturnLimitMessagesWhenLimitGiven() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"limit\":1}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains("username@domain.tld|mailbox|1"));
    }

    @Test
    public void getMessageListShouldReturnLimitMessagesWithDefaultValueWhenLimitIsNotGiven() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test4\r\n\r\ntestmail".getBytes()), new Date(date.toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder("username@domain.tld|mailbox|1", "username@domain.tld|mailbox|2", "username@domain.tld|mailbox|3"));
    }

    @Test
    public void getMessageListShouldChainFetchingMessagesWhenAskedFor() throws Exception {
        jmapServer.serverProbe().createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        jmapServer.serverProbe().appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(date.plusDays(1).toEpochDay()), false, new Flags());
        await();

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\":true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body("[0][0]", equalTo("messageList"))
            .body("[1][0]", equalTo("messages"))
            .body("[0][1].messageIds", hasSize(1))
            .body("[0][1].messageIds[0]", equalTo("username@domain.tld|mailbox|1"))
            .body("[1][1].list", hasSize(1))
            .body("[1][1].list[0].id", equalTo("username@domain.tld|mailbox|1"));
    }
}
