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

package org.apache.james.webadmin.integration;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.CassandraJmapTestRule;
import org.apache.james.DockerCassandraRule;
import org.apache.james.EmbeddedElasticSearchRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.routes.IndexCreationRoutes;
import org.apache.james.webadmin.routes.IndexationRoutes;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public class IndexationRoutesTest {
    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final int SMTP_PORT = 1025;
    public static final String DEFAULT_DOMAIN = "domain.com";
    public static final int IMAP_PORT = 1143;
    public static final String USER = "user@domain.com";
    public static final String PASSWORD = "password";

    @ClassRule
    public static DockerCassandraRule cassandra = new DockerCassandraRule();

    public final EmbeddedElasticSearchRule embeddedElasticSearchRule = new EmbeddedElasticSearchRule();

    @Rule
    public CassandraJmapTestRule cassandraJmapTestRule = CassandraJmapTestRule.defaultTestRule(embeddedElasticSearchRule);

    private GuiceJamesServer guiceJamesServer;
    private ConditionFactory calmlyAwait;

    @Before
    public void setUp() throws Exception {
        guiceJamesServer = cassandraJmapTestRule.jmapServer(cassandra.getModule())
                .overrideWith(new WebAdminConfigurationModule());
        guiceJamesServer.start();
        DataProbe dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        WebAdminGuiceProbe webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        RestAssured.requestSpecification = new RequestSpecBuilder()
        		.setContentType(ContentType.JSON)
        		.setAccept(ContentType.JSON)
        		.setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
                .setPort(webAdminGuiceProbe.getWebAdminPort())
        		.build();

        dataProbe.addDomain(DEFAULT_DOMAIN);
        dataProbe.addUser(USER, PASSWORD);

        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        calmlyAwait = Awaitility.with()
            .pollInterval(slowPacedPollInterval)
            .and()
            .with()
            .pollDelay(slowPacedPollInterval).await();
    }

    @After
    public void tearDown() {
        guiceJamesServer.stop();
    }

    @Test
    public void putShouldAddAnIndexingListener() throws Exception {
        // Given a new index
        String indexName = "my_index";
        String my_alias = "my_alias";
        given()
            .body("{" +
                "   \"schemaVersion\":1," +
                "   \"aliases\":[\"" + my_alias + "\"]" +
                "}")
        .when()
            .put(IndexCreationRoutes.ELASTICSEARCH_INDEX_ENDPOINT + "/" + indexName)
        .then()
            .statusCode(204);

        // When I start indexing on it
        given()
            .body("{" +
                "   \"schemaVersion\":1" +
                "}")
        .when()
            .put(IndexationRoutes.ELASTICSEARCH_INDEXATION_ENDPOINT + "/" + my_alias)
        .then()
            .statusCode(204);

        // And I receive an email
        try (SMTPMessageSender messageSender = SMTPMessageSender.noAuthentication(LOCALHOST_IP, SMTP_PORT, DEFAULT_DOMAIN);
             IMAPMessageReader imapMessageReader = new IMAPMessageReader(LOCALHOST_IP, IMAP_PORT)) {
            String from = USER;
            String to = USER;
            messageSender.sendMessage(from, to);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(messageSender::messageHasBeenSent);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() ->
                imapMessageReader.userReceivedMessageInMailbox(USER, PASSWORD, MailboxConstants.INBOX));
        }
        cassandraJmapTestRule.await();

        // I get one mail indexed on the new index
        SearchHits hits = embeddedElasticSearchRule.getNode()
            .client()
            .prepareSearch(indexName)
            .setQuery(new MatchAllQueryBuilder())
            .execute()
            .actionGet()
            .getHits();

        assertThat(hits).hasSize(1);
    }

}
