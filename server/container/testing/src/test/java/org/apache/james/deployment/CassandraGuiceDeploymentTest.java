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

import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.deployment.Constants.HOMER;
import static org.apache.james.deployment.Constants.HOMER_PASSWORD;
import static org.apache.james.deployment.Constants.IMAP_PORT;
import static org.apache.james.deployment.Constants.JMAP_PORT;
import static org.apache.james.deployment.Constants.LOCALHOST;
import static org.apache.james.deployment.Constants.SMTP_PORT;
import static org.apache.james.deployment.Constants.WEBADMIN_PORT;
import static org.apache.james.deployment.DeploymentTestingOperations.assertImapMessageReceived;
import static org.apache.james.deployment.DeploymentTestingOperations.assertJmapSearchWork;
import static org.apache.james.deployment.DeploymentTestingOperations.assertJmapWorks;
import static org.apache.james.deployment.DeploymentTestingOperations.authenticateJamesUser;
import static org.apache.james.deployment.DeploymentTestingOperations.registerBart;
import static org.apache.james.deployment.DeploymentTestingOperations.registerDomain;
import static org.apache.james.deployment.DeploymentTestingOperations.registerHomer;
import static org.apache.james.deployment.DeploymentTestingOperations.sendMessageFromBartToHomer;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

public class CassandraGuiceDeploymentTest {
    private static final Logger logger = LoggerFactory.getLogger(CassandraGuiceDeploymentTest.class);

    private static final String DEFAULT_CASSANDRA_GUICE_IMAGE = "james-cassandra";
    private static final String CASSANDRA_GUICE_IMAGE = "CASSANDRA_GUICE_IMAGE";

    public Network network = Network.newNetwork();

    public GenericContainer<?> cassandra = new CassandraContainer()
        .withNetwork(network)
        .withNetworkAliases("cassandra");
    
    public GenericContainer<?> elasticsearch = new GenericContainer<>("elasticsearch:2.4.6")
        .withNetwork(network)
        .withNetworkAliases("elasticsearch");

    public GenericContainer<?> james = new GenericContainer<>(resolveImageName())
        .withExposedPorts(JMAP_PORT, SMTP_PORT, IMAP_PORT, WEBADMIN_PORT)
        .withCopyFileToContainer(MountableFile.forClasspathResource("/webadmin.properties"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/jmap.properties"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/keystore"), "/root/conf/")
        .withCopyFileToContainer(MountableFile.forClasspathResource("/jwt_publickey"), "/root/conf/")
        .withNetwork(network)
        .withLogConsumer(log -> logger.info(log.getUtf8String()))
        .waitingFor(WaitStrategies.webAdminWaitStrategy);

    private static Future<String> resolveImageName() {
        return CompletableFuture.supplyAsync(() ->
            Optional.ofNullable(System.getenv(CASSANDRA_GUICE_IMAGE))
                .orElse(DEFAULT_CASSANDRA_GUICE_IMAGE));
    }

    @Rule
    public RuleChain chain = RuleChain.outerRule(network).around(cassandra).around(elasticsearch).around(james);

    private URIBuilder jmapApi;
    
    @Before
    public void setup() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
            .setPort(james.getMappedPort(WEBADMIN_PORT))
            .build();
        jmapApi = new URIBuilder().setScheme("http").setHost(LOCALHOST).setPort(james.getMappedPort(JMAP_PORT));
    }

    @Test
    public void shouldHaveAllServicesResponding() throws Exception {
        registerDomain();
        registerHomer();
        registerBart();

        sendMessageFromBartToHomer(james.getMappedPort(SMTP_PORT));
        assertImapMessageReceived(james.getMappedPort(IMAP_PORT));

        String homerAccessToken = authenticateJamesUser(jmapApi, HOMER, HOMER_PASSWORD);
        assertJmapWorks(jmapApi, homerAccessToken);
        assertJmapSearchWork(jmapApi, homerAccessToken);
    }
}
