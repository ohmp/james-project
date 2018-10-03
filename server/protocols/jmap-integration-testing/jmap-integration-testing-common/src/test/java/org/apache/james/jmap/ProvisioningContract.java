/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
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
import static org.apache.james.jmap.HttpJmapAuthentication.authenticateJamesUser;
import static org.apache.james.jmap.JmapURIBuilder.baseUri;
import static org.apache.james.jmap.TestingConstants.jmapRequestSpecBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.time.Duration;

import org.apache.james.GuiceJamesServer;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public interface ProvisioningContract {
    String NAME = "[0][0]";
    String ARGUMENTS = "[0][1]";
    String DOMAIN = "mydomain.tld";
    String USER = "myuser@" + DOMAIN;
    String PASSWORD = "secret";

    @BeforeEach
    default void setup(GuiceJamesServer server) throws Throwable {
        RestAssured.requestSpecification = jmapRequestSpecBuilder
            .setPort(server.getProbe(JmapGuiceProbe.class).getJmapPort())
            .build();

        server.getProbe(DataProbeImpl.class)
            .fluent()
            .addDomain(DOMAIN)
            .addUser(USER, PASSWORD);
    }

    @Test
    default void provisionMailboxesShouldNotDuplicateMailboxByName(GuiceJamesServer server) throws Exception {
        String token = authenticateJamesUser(baseUri(server), USER, PASSWORD).serialize();

        ConcurrentTestRunner.builder()
            .operation((a, b) -> with()
                .header("Authorization", token)
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
                .post("/jmap"))
            .threadCount(10)
            .runSuccessfullyWithin(Duration.ofMinutes(1));

        given()
            .header("Authorization", token)
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("mailboxes"))
            .body(ARGUMENTS + ".list", hasSize(6))
            .body(ARGUMENTS + ".list.name", hasItems(DefaultMailboxes.DEFAULT_MAILBOXES.toArray()));
    }

    @Test
    default void provisionMailboxesShouldSubscribeToThem(GuiceJamesServer server) throws Exception {
        String token = authenticateJamesUser(baseUri(server), USER, PASSWORD).serialize();

        with()
            .header("Authorization", token)
            .body("[[\"getMailboxes\", {}, \"#0\"]]")
            .post("/jmap");

        assertThat(server.getProbe(MailboxProbeImpl.class)
            .listSubscriptions(USER))
            .containsAll(DefaultMailboxes.DEFAULT_MAILBOXES);
    }
}
