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

package org.apache.james.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.apache.james.deployment.Constants.BART;
import static org.apache.james.deployment.Constants.BART_PASSWORD;
import static org.apache.james.deployment.Constants.HOMER;
import static org.apache.james.deployment.Constants.HOMER_PASSWORD;
import static org.apache.james.deployment.Constants.LOCALHOST;
import static org.apache.james.deployment.Constants.SIMPSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.imap.IMAPClient;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionFactory;
import org.hamcrest.core.IsAnything;

import com.jayway.jsonpath.JsonPath;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;

public class DeploymentTestingOperations {

    private static final Duration slowPacedPollInterval = Duration.ONE_HUNDRED_MILLISECONDS;
    private static final Duration ONE_MILLISECOND = new Duration(1, TimeUnit.MILLISECONDS);

    private static final ConditionFactory calmlyAwait = Awaitility.with()
        .pollInterval(slowPacedPollInterval)
        .and().with()
        .pollDelay(ONE_MILLISECOND)
        .await();

    public static void registerDomain() {
        when()
            .put("/domains/" + SIMPSON)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    public static void registerHomer() {
        given()
            .body(String.format("{\"password\": \"%s\"}", HOMER_PASSWORD))
        .when()
            .put("/users/" + HOMER)
            .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    public static void registerBart() {
        given()
            .body(String.format("{\"password\": \"%s\"}", BART_PASSWORD))
        .when()
            .put("/users/" + BART)
            .then()
            .statusCode(HTTP_NO_CONTENT);
    }


    public static void sendMessageFromBartToHomer(int port)
        throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        AuthenticatingSMTPClient smtpClient = new AuthenticatingSMTPClient();
        try {
            smtpClient.connect(LOCALHOST, port);
            smtpClient.auth(AuthenticatingSMTPClient.AUTH_METHOD.PLAIN, BART, BART_PASSWORD);
            smtpClient.helo(SIMPSON);
            smtpClient.setSender(BART);
            smtpClient.rcpt("<" + HOMER + ">");
            smtpClient.sendShortMessageData("FROM: <" + BART + ">\r\n" +
                "subject: test\r\n" +
                "\r\n" +
                "content\r\n" +
                ".\r\n");
        } finally {
            smtpClient.disconnect();
        }
    }

    public static void assertImapMessageReceived(int port) throws IOException {
        IMAPClient imapClient = new IMAPClient();
        try {
            imapClient.connect(LOCALHOST, port);
            imapClient.login(HOMER, HOMER_PASSWORD);
            imapClient.select("INBOX");

            await().atMost(Duration.TEN_SECONDS).until(() -> hasAMessage(imapClient));
            imapClient.fetch("1:1", "(BODY[])");
            assertThat(imapClient.getReplyString()).contains("FROM: <" + BART + ">");
        } finally {
            imapClient.close();
        }
    }

    public static boolean hasAMessage(IMAPClient imapClient) throws IOException {
        imapClient.fetch("1:1", "ALL");
        return imapClient.getReplyString()
            .contains("OK FETCH completed");
    }

    public static void assertJmapWorks(URIBuilder jmapApi, String homerAccessToken) throws ClientProtocolException, IOException, URISyntaxException {
        Content lastMessageId = Request.Post(jmapApi.setPath("/jmap").build())
            .addHeader("Authorization", homerAccessToken)
            .bodyString("[[\"getMessageList\", {\"sort\":[\"date desc\"]}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
            .execute()
            .returnContent();

        JsonAssertions.assertThatJson(lastMessageId.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }

    public static void assertJmapSearchWork(URIBuilder jmapApi, String homerAccessToken) throws ClientProtocolException, IOException, URISyntaxException {
        Content searchResult = Request.Post(jmapApi.setPath("/jmap").build())
            .addHeader("Authorization", homerAccessToken)
            .bodyString("[[\"getMessageList\", {\"filter\" : {\"text\": \"content\"}}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
            .execute()
            .returnContent();

        JsonAssertions.assertThatJson(searchResult.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }

    public static String authenticateJamesUser(URIBuilder uriBuilder, String username, String password) {
        return calmlyAwait.until(
            () -> doAuthenticate(uriBuilder, username, password), IsAnything.anything());
    }

    public static String doAuthenticate(URIBuilder uriBuilder, String username, String password) throws ClientProtocolException, IOException, URISyntaxException {
        String continuationToken = getContinuationToken(uriBuilder, username);

        Response response = postAuthenticate(uriBuilder, password, continuationToken);

        return JsonPath.parse(response.returnContent().asString())
                .read("accessToken");
    }

    private static Response postAuthenticate(URIBuilder uriBuilder, String password, String continuationToken) throws ClientProtocolException, IOException, URISyntaxException {
        return Request.Post(uriBuilder.setPath("/authentication").build())
            .bodyString("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + password + "\"}",
                ContentType.APPLICATION_JSON)
            .setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType())
            .execute();
    }

    private static String getContinuationToken(URIBuilder uriBuilder, String username) throws ClientProtocolException, IOException, URISyntaxException {
        Response response = Request.Post(uriBuilder.setPath("/authentication").build())
            .bodyString("{\"username\": \"" + username + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}",
                ContentType.APPLICATION_JSON)
            .setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType())
            .execute();

        return JsonPath.parse(response.returnContent().asString())
            .read("continuationToken");
    }
}
