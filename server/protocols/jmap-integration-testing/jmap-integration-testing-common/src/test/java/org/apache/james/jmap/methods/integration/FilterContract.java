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
import static org.apache.james.jmap.HttpJmapAuthentication.authenticateJamesUser;
import static org.apache.james.jmap.JmapCommonRequests.getOutboxId;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.ALICE;
import static org.apache.james.jmap.TestingConstants.ALICE_PASSWORD;
import static org.apache.james.jmap.TestingConstants.ARGUMENTS;
import static org.apache.james.jmap.TestingConstants.BOB;
import static org.apache.james.jmap.TestingConstants.BOB_PASSWORD;
import static org.apache.james.jmap.TestingConstants.CEDRIC;
import static org.apache.james.jmap.TestingConstants.DOMAIN;
import static org.apache.james.jmap.TestingConstants.NAME;
import static org.apache.james.jmap.TestingConstants.calmlyAwait;
import static org.apache.james.jmap.TestingConstants.jmapRequestSpecBuilder;
import static org.apache.james.mailbox.model.MailboxConstants.INBOX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Locale;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapCommonRequests;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class FilterContract {
    protected abstract MailboxId randomMailboxId();

    private AccessToken accessToken;
    private AccessToken bobAccessToken;

    private MailboxId matchedMailbox;
    private MailboxId inbox;

    @BeforeEach
    void setup(GuiceJamesServer server) throws Exception {
        RestAssured.requestSpecification = jmapRequestSpecBuilder
            .setPort(server.getProbe(JmapGuiceProbe.class).getJmapPort())
            .build();

        server.getProbe(DataProbeImpl.class)
            .fluent()
            .addDomain(DOMAIN)
            .addUser(ALICE, ALICE_PASSWORD)
            .addUser(BOB, BOB_PASSWORD);
        accessToken = authenticateJamesUser(baseUri(server), ALICE, ALICE_PASSWORD);
        bobAccessToken = authenticateJamesUser(baseUri(server), BOB, BOB_PASSWORD);

        MailboxProbeImpl mailboxProbe = server.getProbe(MailboxProbeImpl.class);
        matchedMailbox = mailboxProbe.createMailbox(MailboxPath.forUser(ALICE, "matched"));
        inbox = mailboxProbe.createMailbox(MailboxPath.forUser(ALICE, INBOX));
    }

    @Test
    void getFilterShouldReturnEmptyByDefault() {
        String body = "[[" +
                "  \"getFilter\", " +
                "  {}, " +
                "\"#0\"" +
                "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(body)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(0));
    }

    @Test
    void getFilterShouldReturnEmptyWhenExplicitNullAccountId() {
        String body = "[[" +
                "  \"getFilter\", " +
                "  {\"accountId\": null}, " +
                "\"#0\"" +
                "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(body)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(0));
    }

    @Test
    void getFilterShouldReturnErrorWhenUnsupportedAccountId() {
        String body = "[[" +
                "  \"getFilter\", " +
                "  {\"accountId\": \"any\"}, " +
                "\"#0\"" +
                "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(body)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("The field 'accountId' of 'GetFilterRequest' is not supported"));
    }

    @Test
    void setFilterShouldOverwritePreviouslyStoredRules() {
        MailboxId mailbox1 = randomMailboxId();
        MailboxId mailbox2 = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My first rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox2.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        with()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"42-ac\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"from\"," +
                  "        \"comparator\": \"exactly-equals\"," +
                  "        \"value\": \"marvin@h2.g2\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "            \"mailboxIds\": [\"" + mailbox1.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"getFilter\", " +
                  "  {}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(1))
            .body(ARGUMENTS + ".singleton[0].id", equalTo("42-ac"))
            .body(ARGUMENTS + ".singleton[0].name", equalTo("My last rule"))
            .body(ARGUMENTS + ".singleton[0].condition.field", equalTo("from"))
            .body(ARGUMENTS + ".singleton[0].condition.comparator", equalTo("exactly-equals"))
            .body(ARGUMENTS + ".singleton[0].condition.value", equalTo("marvin@h2.g2"))
            .body(ARGUMENTS + ".singleton[0].action.appendIn.mailboxIds", containsInAnyOrder(mailbox1.serialize()));
    }

    @Test
    void setFilterShouldReturnUpdatedSingleton() {
        MailboxId mailbox = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("filterSet"))
            .body(ARGUMENTS + ".updated", containsInAnyOrder("singleton"));
    }

    @Test
    void setFilterShouldRejectDuplicatedRules() {
        MailboxId mailbox = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("filterSet"))
            .body(ARGUMENTS + ".updated", hasSize(0))
            .body(ARGUMENTS + ".notUpdated.singleton.type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".notUpdated.singleton.description", equalTo("The following rules were duplicated: ['3000-34e']"));
    }

    @Test
    void setFilterShouldRejectRulesTargetingSeveralMailboxes() {
        MailboxId mailbox1 = randomMailboxId();
        MailboxId mailbox2 = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox1.serialize() + "\",\"" + mailbox2.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("filterSet"))
            .body(ARGUMENTS + ".updated", hasSize(0))
            .body(ARGUMENTS + ".notUpdated.singleton.type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".notUpdated.singleton.description", equalTo("The following rules targeted several mailboxes, which is not supported: ['3000-34e']"));
    }

    @Test
    void setFilterShouldRejectAccountId() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"accountId\": \"any\"," +
                  "    \"singleton\": []" +
                  "  }, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("The field 'accountId' of 'SetFilterRequest' is not supported"));
    }

    @Test
    void setFilterShouldAcceptNullAccountId() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"accountId\": null," +
                  "    \"singleton\": []" +
                  "  }, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("filterSet"))
            .body(ARGUMENTS + ".updated", hasSize(1));
    }

    @Test
    void setFilterShouldRejectIfInState() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"ifInState\": \"any\"," +
                  "    \"singleton\": []" +
                  "  }, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("The field 'ifInState' of 'SetFilterRequest' is not supported"));
    }

    @Test
    void setFilterShouldAcceptNullIfInState() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"ifInState\": null," +
                  "    \"singleton\": []" +
                  "  }, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .body(NAME, equalTo("filterSet"))
            .body(ARGUMENTS + ".updated", hasSize(1));
    }

    @Test
    void getFilterShouldRetrievePreviouslyStoredRules() {
        MailboxId mailbox1 = randomMailboxId();
        MailboxId mailbox2 = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"42-ac\"," +
                  "      \"name\": \"My first rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"from\"," +
                  "        \"comparator\": \"exactly-equals\"," +
                  "        \"value\": \"marvin@h2.g2\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "            \"mailboxIds\": [\"" + mailbox1.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox2.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"getFilter\", " +
                  "  {}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(2))
            .body(ARGUMENTS + ".singleton[0].id", equalTo("42-ac"))
            .body(ARGUMENTS + ".singleton[0].name", equalTo("My first rule"))
            .body(ARGUMENTS + ".singleton[0].condition.field", equalTo("from"))
            .body(ARGUMENTS + ".singleton[0].condition.comparator", equalTo("exactly-equals"))
            .body(ARGUMENTS + ".singleton[0].condition.value", equalTo("marvin@h2.g2"))
            .body(ARGUMENTS + ".singleton[0].action.appendIn.mailboxIds", containsInAnyOrder(mailbox1.serialize()))
            .body(ARGUMENTS + ".singleton[1].id", equalTo("3000-34e"))
            .body(ARGUMENTS + ".singleton[1].name", equalTo("My last rule"))
            .body(ARGUMENTS + ".singleton[1].condition.field", equalTo("subject"))
            .body(ARGUMENTS + ".singleton[1].condition.comparator", equalTo("contains"))
            .body(ARGUMENTS + ".singleton[1].condition.value", equalTo("question"))
            .body(ARGUMENTS + ".singleton[1].action.appendIn.mailboxIds", containsInAnyOrder(mailbox2.serialize()));
    }

    @Test
    void setFilterShouldClearPreviouslyStoredRulesWhenEmptyBody() {
        MailboxId mailbox = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-34e\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        with()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": []" +
                  "  }, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"getFilter\", " +
                  "  {}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(0));
    }

    @Test
    void allFieldsAndComparatorsShouldBeSupported() {
        MailboxId mailbox = randomMailboxId();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"setFilter\", " +
                  "  {" +
                  "    \"singleton\": [" +
                  "    {" +
                  "      \"id\": \"3000-341\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"subject\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-342\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"cc\"," +
                  "        \"comparator\": \"not-contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-343\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"to\"," +
                  "        \"comparator\": \"exactly-equals\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-344\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"recipient\"," +
                  "        \"comparator\": \"not-exactly-equals\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }," +
                  "    {" +
                  "      \"id\": \"3000-345\"," +
                  "      \"name\": \"My last rule\"," +
                  "      \"condition\": {" +
                  "        \"field\": \"from\"," +
                  "        \"comparator\": \"contains\"," +
                  "        \"value\": \"question\"" +
                  "      }," +
                  "      \"action\": {" +
                  "        \"appendIn\": {" +
                  "          \"mailboxIds\": [\"" + mailbox.serialize() + "\"]" +
                  "        }" +
                  "      }" +
                  "    }" +
                  "  ]}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                  "  \"getFilter\", " +
                  "  {}, " +
                  "\"#0\"" +
                  "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("filter"))
            .body(ARGUMENTS + ".singleton", hasSize(5))
            .body(ARGUMENTS + ".singleton[0].id", equalTo("3000-341"))
            .body(ARGUMENTS + ".singleton[0].condition.field", equalTo("subject"))
            .body(ARGUMENTS + ".singleton[0].condition.comparator", equalTo("contains"))
            .body(ARGUMENTS + ".singleton[1].id", equalTo("3000-342"))
            .body(ARGUMENTS + ".singleton[1].condition.field", equalTo("cc"))
            .body(ARGUMENTS + ".singleton[1].condition.comparator", equalTo("not-contains"))
            .body(ARGUMENTS + ".singleton[2].id", equalTo("3000-343"))
            .body(ARGUMENTS + ".singleton[2].condition.field", equalTo("to"))
            .body(ARGUMENTS + ".singleton[2].condition.comparator", equalTo("exactly-equals"))
            .body(ARGUMENTS + ".singleton[3].id", equalTo("3000-344"))
            .body(ARGUMENTS + ".singleton[3].condition.field", equalTo("recipient"))
            .body(ARGUMENTS + ".singleton[3].condition.comparator", equalTo("not-exactly-equals"))
            .body(ARGUMENTS + ".singleton[4].id", equalTo("3000-345"))
            .body(ARGUMENTS + ".singleton[4].condition.field", equalTo("from"))
            .body(ARGUMENTS + ".singleton[4].condition.comparator", equalTo("contains"));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenFromRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + BOB + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap");

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenToRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"to\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"},{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenCcRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails cc-ed to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"cc\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"cc\": [{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenRecipientRuleMatchesCc() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails cc-ed to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"recipient\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"cc\": [{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenRecipientRuleMatchesTo() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails cc-ed to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"recipient\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"cc\": [{ \"name\": \"Cedric\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenSubjectRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"subject\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"matchme\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"},{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"matchme\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenFromDoesNotMatchRule() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenToDoesNotMatchRule() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"to\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenCcDoesNotMatchRule() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails cc-ed to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"cc\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"cc\": [{ \"name\": \"Cedric\", \"email\": \"" + BOB + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenRecipientDoesNotMatchRule() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails cc-ed to cedric\"," +
                "      \"condition\": {" +
                "        \"field\": \"recipient\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + CEDRIC + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"cc\": [{ \"name\": \"Cedric\", \"email\": \"" + BOB + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenSubjectRuleDoesNotMatchRule() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"subject\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"matchme\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"},{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"nomatch\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }


    @Test
    void messageShouldBeAppendedInInboxWhenSubjectRuleDoesNotMatchRuleBecaseOfCase() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"subject\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"different case value\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"},{ \"name\": \"Cedric\", \"email\": \"" + CEDRIC + "\"}]," +
            "      \"subject\": \"DIFFERENT CASE VALUE\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenContainsComparatorMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"bo\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenContainsComparatorDoesNotMatch() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"ced\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenExactlyEqualsMatchesName() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"exactly-equals\"," +
                "        \"value\": \"Bob\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenExactlyEqualsMatchesAddress() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"exactly-equals\"," +
                "        \"value\": \"" + BOB + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenExactlyEqualsMatchesFullHeader() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"exactly-equals\"," +
                "        \"value\": \"Bob <" + BOB + ">\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }


    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenExactlyEqualsMatchesCaseInsensitivelyFullHeader() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"exactly-equals\"," +
                "        \"value\": \"bob <" + BOB.toUpperCase(Locale.ENGLISH) + ">\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenExactlyEqualsComparatorDoesNotMatch() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"exactly-equals\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"nomatch\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenNotContainsComparatorMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"not-contains\"," +
                "        \"value\": \"other\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenNotContainsComparatorDoesNotMatch() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"not-contains\"," +
                "        \"value\": \"bob\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenContainsNotExactlyEqualsMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"not-exactly-equals\"," +
                "        \"value\": \"nomatch\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenNotExactlyEqualsMatchesAddress() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"not-exactly-equals\"," +
                "        \"value\": \"" + BOB + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenNotExactlyEqualsMatchesFullHeader() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"not-exactly-equals\"," +
                "        \"value\": \"Bob <" + BOB + ">\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Bob\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInInboxWhenNotExactlyEqualsComparatorMatchesName() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"not-exactly-equals\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"Bob\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap")
        .then()
            .statusCode(200);

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenFirstRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + BOB + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }," +
                "    {" +
                "      \"id\": \"3000-346\"," +
                "      \"name\": \"Emails to alice\"," +
                "      \"condition\": {" +
                "        \"field\": \"to\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + ALICE + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap");

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void messageShouldBeAppendedInSpecificMailboxWhenSecondRuleMatches() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"unknown@james.org\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }," +
                "    {" +
                "      \"id\": \"3000-346\"," +
                "      \"name\": \"Emails to alice\"," +
                "      \"condition\": {" +
                "        \"field\": \"to\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + ALICE + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap");

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
    }

    @Test
    void inboxShouldBeEmptyWhenFromRuleMatchesInSpecificMailbox() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"" + BOB + "\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap");

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox));
        assertThat(JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox)).isFalse();
    }

    @Test
    void matchedMailboxShouldBeEmptyWhenFromRuleDoesntMatch() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[" +
                "  \"setFilter\", " +
                "  {" +
                "    \"singleton\": [" +
                "    {" +
                "      \"id\": \"3000-345\"," +
                "      \"name\": \"Emails from bob\"," +
                "      \"condition\": {" +
                "        \"field\": \"from\"," +
                "        \"comparator\": \"contains\"," +
                "        \"value\": \"unknown@james.org\"" +
                "      }," +
                "      \"action\": {" +
                "        \"appendIn\": {" +
                "          \"mailboxIds\": [\"" + matchedMailbox.serialize() + "\"]" +
                "        }" +
                "      }" +
                "    }" +
                "  ]}, " +
                "\"#0\"" +
                "]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200);

        String messageCreationId = "creationId1337";
        String requestBody = "[[" +
            "  \"setMessages\"," +
            "  {" +
            "    \"create\": { \"" + messageCreationId  + "\" : {" +
            "      \"from\": { \"name\": \"Me\", \"email\": \"" + BOB + "\"}," +
            "      \"to\": [{ \"name\": \"Alice\", \"email\": \"" + ALICE + "\"}]," +
            "      \"subject\": \"subject\"," +
            "      \"mailboxIds\": [\"" + getOutboxId(bobAccessToken) + "\"]" +
            "    }}" +
            "  }," +
            "  \"#0\"" +
            "]]";

        with()
            .header("Authorization", bobAccessToken.serialize())
            .body(requestBody)
            .post("/jmap");

        calmlyAwait.until(
            () -> JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, inbox));
        assertThat(JmapCommonRequests.isAnyMessageFoundInRecipientsMailbox(accessToken, matchedMailbox)).isFalse();
    }
}
