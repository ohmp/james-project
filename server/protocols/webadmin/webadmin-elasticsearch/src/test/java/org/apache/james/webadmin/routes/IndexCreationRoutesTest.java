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

package org.apache.james.webadmin.routes;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.james.backends.es.EmbeddedElasticSearch;
import org.apache.james.mailbox.elasticsearch.MailboxElasticSearchConstants;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJsonV1;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcherV1;
import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public class IndexCreationRoutesTest {
    private WebAdminServer webAdminServer;

    private TemporaryFolder temporaryFolder = new TemporaryFolder();
    private EmbeddedElasticSearch embeddedElasticSearch =
        new EmbeddedElasticSearch(temporaryFolder, MailboxElasticSearchConstants.DEFAULT_MAILBOX_INDEX);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder).around(embeddedElasticSearch);

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(
            binder -> binder.bind(Client.class).toInstance(embeddedElasticSearch.getNode().client()),
            binder -> binder.bind(MessageToElasticSearchJsonV1.class).toInstance(mock(MessageToElasticSearchJsonV1.class)),
            binder -> binder.bind(ElasticSearchSearcherV1.class).toInstance(mock(ElasticSearchSearcherV1.class)));

        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            injector.getInstance(IndexCreationRoutes.class));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .setPort(webAdminServer.getPort().toInt())
            .build();
    }

    @Test
    public void test() {
        String indexName = "my_index";
        given()
            .body("{" +
                "   \"schemaVersion\":1" +
                "}")
        .when()
            .put(IndexCreationRoutes.ELASTICSEARCH_INDEX_ENDPOINT + "/" + indexName)
        .then()
            .statusCode(204);

        assertThat(embeddedElasticSearch.getNode()
            .client()
            .admin()
            .indices()
            .getIndex(new GetIndexRequest()
                .indices(indexName))
            .actionGet()
            .getIndices())
            .contains(indexName);
    }
}
