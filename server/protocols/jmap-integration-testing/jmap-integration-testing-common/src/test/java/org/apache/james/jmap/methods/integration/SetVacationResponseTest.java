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
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.api.vacation.AccountId;
import org.apache.james.jmap.api.vacation.Vacation;
import org.apache.james.jmap.api.vacation.VacationPatch;
import org.apache.james.util.ValuePatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public abstract class SetVacationResponseTest {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final String USERS_DOMAIN = "domain.tld";
    public static final String USER = "username@" + USERS_DOMAIN;
    public static final String PASSWORD = "password";
    public static final String SUBJECT = "subject";

    protected abstract GuiceJamesServer createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private GuiceJamesServer jmapServer;

    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        RestAssured.requestSpecification = new RequestSpecBuilder()
        		.setContentType(ContentType.JSON)
        		.setAccept(ContentType.JSON)
        		.setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
        		.setPort(jmapServer.getJmapPort())
        		.build();

        jmapServer.serverProbe().addDomain(USERS_DOMAIN);
        jmapServer.serverProbe().addUser(USER, PASSWORD);
        accessToken = JmapAuthentication.authenticateJamesUser(USER, PASSWORD);

        await();
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }

    @Test
    public void setVacationResponseShouldReturnErrorOnMalformedRequestStructure() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"idVacation\" : {" +
                        "\"id\": \"1\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".description", equalTo("update field should just contain one entry with key \"singleton\""));
    }

    @Test
    public void setVacationResponseShouldBeAbleToContainIsActivated() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
                "{" +
                    "\"update\":{" +
                        "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"isActivated\": \"true\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));
    }

    @Test
    public void setVacationResponseShouldContainAnErrorWhenInvalidId() {
        int id = 1;
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
            "\"id\": \"" + id + "\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".notUpdated.singleton.type", equalTo("invalidArguments"))
            .body(ARGUMENTS + ".notUpdated.singleton.description", equalTo("There is one VacationResponse object per account, with id set to \\\"singleton\\\" and not to " + id));
    }

    @Test
    public void setVacationResponseShouldReturnCorrectAnswerUponValidVacationResponse() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"," +
                        "\"htmlBody\": \"<p>Here is the HTML version</p>\"," +
                        "\"fromDate\":\"2014-09-30T14:10:00Z[GMT]\"," +
                        "\"toDate\":\"2014-10-30T14:10:00Z[GMT]\"," +
                        "\"subject\":\"" + SUBJECT + "\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));

        Vacation vacation = jmapServer.serverProbe().retrieveVacation(AccountId.fromString(USER));
        assertThat(vacation.getTextBody()).contains("Message explaining my wonderful vacations");
        assertThat(vacation.getHtmlBody()).contains("<p>Here is the HTML version</p>");
        assertThat(vacation.isEnabled()).isTrue();
        assertThat(vacation.getFromDate()).contains(ZonedDateTime.parse("2014-09-30T14:10:00Z[GMT]"));
        assertThat(vacation.getToDate()).contains(ZonedDateTime.parse("2014-10-30T14:10:00Z[GMT]"));
        assertThat(vacation.getSubject()).contains(SUBJECT);
    }

    @Test
    public void setVacationResponseShouldAllowResets() {
        jmapServer.serverProbe().modifyVacation(AccountId.fromString(USER),
            VacationPatch.builder()
                .textBody(ValuePatch.modifyTo("any value"))
                .build());

        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"textBody\": null" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));

        assertThat(jmapServer.serverProbe().retrieveVacation(AccountId.fromString(USER)))
            .isEqualTo(Vacation.builder()
                .enabled(false)
                .build());
    }

    @Test
    public void setVacationResponseShouldNotAlterAbsentProperties() {
        String textBody = "any value";
        String subject = "any subject";
        jmapServer.serverProbe().modifyVacation(AccountId.fromString(USER),
            VacationPatch.builder()
                .textBody(ValuePatch.modifyTo(textBody))
                .build());

        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"subject\": \"" + subject + "\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
            .when()
            .post("/jmap")
            .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));

        assertThat(jmapServer.serverProbe().retrieveVacation(AccountId.fromString(USER)))
            .isEqualTo(Vacation.builder()
                .enabled(false)
                .subject(Optional.of(subject))
                .textBody(textBody)
                .build());
    }

    @Test
    public void setVacationResponseShouldAllowPartialUpdates() {
        jmapServer.serverProbe().modifyVacation(AccountId.fromString(USER),
            VacationPatch.builder()
                .textBody(ValuePatch.modifyTo("any value"))
                .build());

        String newTextBody = "Awesome text message 2";
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"textBody\": \"" + newTextBody + "\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
            .when()
            .post("/jmap")
            .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));

        assertThat(jmapServer.serverProbe().retrieveVacation(AccountId.fromString(USER)))
            .isEqualTo(Vacation.builder()
                .enabled(false)
                .textBody(newTextBody)
                .build());
    }

    @Test
    public void setVacationResponseShouldHandleNamedTimeZone() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"," +
                        "\"fromDate\":\"2016-04-03T02:01+07:00[Asia/Vientiane]\"," +
                        "\"toDate\":\"2016-04-07T02:01+07:00[Asia/Vientiane]\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
            .when()
            .post("/jmap")
            .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));

        Vacation vacation = jmapServer.serverProbe().retrieveVacation(AccountId.fromString(USER));
        assertThat(vacation.getTextBody()).contains("Message explaining my wonderful vacations");
        assertThat(vacation.isEnabled()).isTrue();
        assertThat(vacation.getFromDate()).contains(ZonedDateTime.parse("2016-04-03T02:01+07:00[Asia/Vientiane]"));
        assertThat(vacation.getToDate()).contains(ZonedDateTime.parse("2016-04-07T02:01+07:00[Asia/Vientiane]"));
    }

    @Test
    public void accountIdIsNotSupported() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"accountId\": \"1\"," +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"," +
                        "\"fromDate\":\"2014-09-30T14:10:00Z\"," +
                        "\"toDate\":\"2014-10-30T14:10:00Z\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("Not yet implemented"));
    }

    @Test
    public void accountIdNullIsSupported() {
        String bodyRequest = "[[" +
            "\"setVacationResponse\", " +
            "{" +
                "\"accountId\": null," +
                "\"update\":{" +
                    "\"singleton\" : {" +
                        "\"id\": \"singleton\"," +
                        "\"isEnabled\": \"true\"," +
                        "\"textBody\": \"Message explaining my wonderful vacations\"," +
                        "\"fromDate\":\"2014-09-30T14:10:00Z\"," +
                        "\"toDate\":\"2014-10-30T14:10:00Z\"" +
                    "}" +
                "}" +
            "}, " +
            "\"#0\"" +
            "]]";

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", accessToken.serialize())
            .body(bodyRequest)
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("vacationResponseSet"))
            .body(ARGUMENTS + ".updated[0]", equalTo("singleton"));
    }
}
