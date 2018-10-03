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
package org.apache.james.jmap;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.apache.james.jmap.TestingConstants.jmapRequestSpecBuilder;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.model.ContinuationToken;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public interface JMAPAuthenticationContract {
    ZonedDateTime oldDate = ZonedDateTime.parse("2011-12-03T10:15:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime newDate = ZonedDateTime.parse("2011-12-03T10:16:30+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    ZonedDateTime afterExpirationDate = ZonedDateTime.parse("2011-12-03T10:30:31+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    FixedDateZonedDateTimeProvider ZONED_DATE_TIME_PROVIDER = new FixedDateZonedDateTimeProvider();

    UserCredentials userCredentials = UserCredentials.builder()
        .username("user@domain.tld")
        .password("password")
        .build();

    @BeforeEach
    default void setup(GuiceJamesServer server) throws Exception {
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(oldDate);

        RestAssured.requestSpecification = jmapRequestSpecBuilder
                .setPort(server.getProbe(JmapGuiceProbe.class).getJmapPort())
                .build();
        
        String domain = "domain.tld";
        server.getProbe(DataProbeImpl.class)
            .fluent()
            .addDomain(domain)
            .addUser(userCredentials.getUsername(), userCredentials.getPassword());
    }

    @Test
    default void mustReturnMalformedRequestWhenContentTypeIsMissing() {
        given()
            .accept(ContentType.JSON)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenContentTypeIsNotJson() {
        given()
            .contentType(ContentType.XML)
            .accept(ContentType.JSON)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenAcceptIsMissing() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenAcceptIsNotJson() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.XML)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenCharsetIsNotUTF8() {
        given()
            .contentType("application/json; charset=ISO-8859-1")
            .accept(ContentType.JSON)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenBodyIsEmpty() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnMalformedRequestWhenBodyIsNotAcceptable() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"badAttributeName\": \"value\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    @Test
    default void mustReturnJsonResponse() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + userCredentials.getUsername() + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    default void methodShouldContainPasswordWhenValidResquest() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + userCredentials.getUsername() + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(200)
            .body("methods", hasItem(userCredentials.getPassword()));
    }

    @Test
    default void mustReturnContinuationTokenWhenValidResquest() {
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + userCredentials.getUsername() + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(200)
            .body("continuationToken", isA(String.class));
    }

    @Test
    default void mustReturnAuthenticationFailedWhenBadPassword() {
        String continuationToken = fromGoodContinuationTokenRequest();

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"badpassword\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void mustReturnAuthenticationFailedWhenContinuationTokenIsRejectedByTheContinuationTokenManager() {
        ContinuationToken badContinuationToken = new ContinuationToken(userCredentials.getUsername(), newDate, "badSignature");

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + badContinuationToken.serialize() + "\", \"method\": \"password\", \"password\": \"" + userCredentials.getPassword() + "\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void mustReturnRestartAuthenticationWhenContinuationTokenIsExpired() {
        String continuationToken = fromGoodContinuationTokenRequest();
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(afterExpirationDate);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + userCredentials.getPassword() + "\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(403);
    }

    @Test
    default void mustReturnAuthenticationFailedWhenUsersRepositoryException() {
        String continuationToken = fromGoodContinuationTokenRequest();

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + "wrong password" + "\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void mustReturnCreatedWhenGoodPassword() {
        String continuationToken = fromGoodContinuationTokenRequest();
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(newDate);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + userCredentials.getPassword() + "\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(201);
    }

    @Test
    default void mustSendJsonContainingAccessTokenAndEndpointsWhenGoodPassword() {
        String continuationToken = fromGoodContinuationTokenRequest();
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(newDate);

        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + userCredentials.getPassword() + "\"}")
        .when()
            .post("/authentication")
        .then()
            .body("accessToken", isA(String.class))
            .body("api", equalTo("/jmap"))
            .body("eventSource", both(isA(String.class)).and(notNullValue()))
            .body("upload", equalTo("/upload"))
            .body("download", equalTo("/download"));
    }
    
    @Test
    default void getMustReturnUnauthorizedWithoutAuthroizationHeader() {
        given()
        .when()
            .get("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void getMustReturnUnauthorizedWithoutAValidAuthroizationHeader() {
        given()
            .header("Authorization", UUID.randomUUID())
        .when()
            .get("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void getMustReturnEndpointsWhenValidAuthorizationHeader() {
        String continuationToken = fromGoodContinuationTokenRequest();
        String token = fromGoodAccessTokenRequest(continuationToken);

        given()
            .header("Authorization", token)
        .when()
            .get("/authentication")
        .then()
            .statusCode(200)
            .body("api", equalTo("/jmap"))
            .body("eventSource", both(isA(String.class)).and(notNullValue()))
            .body("upload", equalTo("/upload"))
            .body("download", equalTo("/download"));
    }

    @Test
    default void getMustReturnEndpointsWhenValidJwtAuthorizationHeader() {
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.T04BTk" +
                "LXkJj24coSZkK13RfG25lpvmSl2MJ7N10KpBk9_-95EGYZdog-BDAn3PJzqVw52z-Bwjh4VOj1-j7cURu0cT4jXehhUrlCxS4n7QHZ" +
                "DN_bsEYGu7KzjWTpTsUiHe-rN7izXVFxDGG1TGwlmBCBnPW-EFCf9ylUsJi0r2BKNdaaPRfMIrHptH1zJBkkUziWpBN1RNLjmvlAUf" +
                "49t1Tbv21ZqYM5Ht2vrhJWczFbuC-TD-8zJkXhjTmA1GVgomIX5dx1cH-dZX1wANNmshUJGHgepWlPU-5VIYxPEhb219RMLJIELMY2" +
                "qNOR8Q31ydinyqzXvCSzVJOf6T60-w";

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/authentication")
                .then()
                .statusCode(200);
    }

    @Test
    default void getMustReturnEndpointsWhenValidUnkwnonUserJwtAuthorizationHeader() {
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMzM3IiwibmFtZSI6Ik5ldyBVc2VyIn0.ci8U04EOWKpi_y"
                + "faKs8fnCBcu1mWs8fvf-t9SDP2kkvDDfD-ya4sEGn4ueCp2dA2ndefZfVu_IfvdlVtxqzSf0tQ-dFKrIe-OtSKhI2otjWctLtk9A"
                + "G7jpWkXoDgr5IOVmsqg37Zxc2bgkLkC5FJqV6oCp51TNQTH6zZbXIUeuGFbHj2-iJeX8sACKTQB0llwc6TFm7GYUF03rv4DfJjqp"
                + "Kd0g8RdnlevSOjV-gGzvKEItugtexS5pgOZ2GYcvqEUDb9EnQR7Qe2EzPAX_FCJfGhlv7bDQlTgOHHAjqw2lD4-zeAznw-3wlYLS"
                + "zhi4ivvPjT-y2T5wnnhzeeYOpYOQ";
        
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/authentication")
        .then()
            .statusCode(200);
    }
    
    @Test
    default void getMustReturnBadCredentialsWhenInvalidJwtAuthorizationHeader() {
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.T04BTk" +
                "LXkJj24coSZkK13RfG25lpvmSl2MJ7N10KpBk9_-95EGYZdog-BDAn3PJzqVw52z-Bwjh4VOj1-j7cURu0cT4jXehhUrlCxS4n7QHZ" +
                "EN_bsEYGu7KzjWTpTsUiHe-rN7izXVFxDGG1TGwlmBCBnPW-EFCf9ylUsJi0r2BKNdaaPRfMIrHptH1zJBkkUziWpBN1RNLjmvlAUf" +
                "49t1Tbv21ZqYM5Ht2vrhJWczFbuC-TD-8zJkXhjTmA1GVgomIX5dx1cH-dZX1wANNmshUJGHgepWlPU-5VIYxPEhb219RMLJIELMY2" +
                "qNOR8Q31ydinyqzXvCSzVJOf6T60-w";

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/authentication")
                .then()
                .statusCode(401);
    }

    @Test
    default void optionsRequestsShouldNeverRequireAuthentication() {
        given()
                .when()
                .options("/authentication")
                .then()
                .statusCode(200);
    }

    @Test
    default void getMustReturnEndpointsWhenCorrectAuthentication() {
        String continuationToken = fromGoodContinuationTokenRequest();
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(newDate);
    
        String accessToken = fromGoodAccessTokenRequest(continuationToken);
    
        given()
            .header("Authorization", accessToken)
        .when()
            .get("/authentication")
        .then()
            .statusCode(200)
            .body("api", isA(String.class));
    }
    
    @Test
    default void deleteMustReturnUnauthenticatedWithoutAuthorizationHeader() {
        given()
        .when()
            .delete("/authentication")
        .then()
            .statusCode(401);
    }

    @Test
    default void deleteMustReturnUnauthenticatedWithoutAValidAuthroizationHeader() {
        given()
            .header("Authorization", UUID.randomUUID())
        .when()
            .delete("/authentication")
        .then()
            .statusCode(401);
    }
    
    @Test
    default void deleteMustReturnOKNoContentOnValidAuthorizationToken() {
        String continuationToken = fromGoodContinuationTokenRequest();
        String token = fromGoodAccessTokenRequest(continuationToken);
        given()
            .header("Authorization", token)
        .when()
            .delete("/authentication")
        .then()
            .statusCode(204);
    }

    @Test
    default void deleteMustInvalidAuthorizationOnCorrectAuthorization() {
        String continuationToken = fromGoodContinuationTokenRequest();
        ZONED_DATE_TIME_PROVIDER.setFixedDateTime(newDate);
    
        String accessToken = fromGoodAccessTokenRequest(continuationToken);
        
        goodDeleteAccessTokenRequest(accessToken);
    
        given()
            .header("Authorization", accessToken)
        .when()
            .get("/authentication")
        .then()
            .statusCode(401);
    }

    default String fromGoodContinuationTokenRequest() {
        return with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"username\": \"" + userCredentials.getUsername() + "\", \"clientName\": \"Mozilla Thunderbird\", \"clientVersion\": \"42.0\", \"deviceName\": \"Joe Blogg’s iPhone\"}")
        .post("/authentication")
            .body()
            .path("continuationToken")
            .toString();
    }

    default String fromGoodAccessTokenRequest(String continuationToken) {
        return with()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("{\"token\": \"" + continuationToken + "\", \"method\": \"password\", \"password\": \"" + userCredentials.getPassword() + "\"}")
        .post("/authentication")
            .path("accessToken")
            .toString();
    }

    default void goodDeleteAccessTokenRequest(String accessToken) {
        with()
            .header("Authorization", accessToken)
            .delete("/authentication");
    }
}
