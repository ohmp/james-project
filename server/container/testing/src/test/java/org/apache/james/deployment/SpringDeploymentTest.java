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

import static org.apache.james.deployment.Constants.BART;
import static org.apache.james.deployment.Constants.BART_PASSWORD;
import static org.apache.james.deployment.Constants.HOMER;
import static org.apache.james.deployment.Constants.HOMER_PASSWORD;
import static org.apache.james.deployment.Constants.IMAP_PORT;
import static org.apache.james.deployment.Constants.JMAP_PORT;
import static org.apache.james.deployment.Constants.SIMPSON;
import static org.apache.james.deployment.Constants.SMTP_PORT;
import static org.apache.james.deployment.Constants.WEBADMIN_PORT;
import static org.apache.james.deployment.DeploymentTestingOperations.assertImapMessageReceived;
import static org.apache.james.deployment.DeploymentTestingOperations.sendMessageFromBartToHomer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class SpringDeploymentTest {
    private static final Logger logger = LoggerFactory.getLogger(JpaGuiceDeploymentTest.class);

    private static final String SPRING_IMAGE = "SPRING_IMAGE";

    private static final String CLI = "/root/james-server-app-3.2.0-SNAPSHOT/bin/james-cli.sh";

    @Rule
    public GenericContainer<?> james = jamesSpring();

    private GenericContainer<?> jamesSpring() {
        GenericContainer<?> james = new GenericContainer<>(resolveImageName())
            .withExposedPorts(JMAP_PORT, SMTP_PORT, IMAP_PORT, WEBADMIN_PORT)
            .withCopyFileToContainer(MountableFile.forClasspathResource("/keystore"), "/root/conf/")
            .withLogConsumer(log -> logger.info(log.getUtf8String()));

        return james.waitingFor(WaitStrategies.cliWaitStrategy(james));
    }

    private static Future<String> resolveImageName() {
        return CompletableFuture.supplyAsync(() ->
            Optional.ofNullable(System.getenv(SPRING_IMAGE))
                .orElseThrow(() -> new RuntimeException("No default images for Spring deployment tests")));
    }

    @Test
    public void shouldHaveAllServicesResponding() throws Exception {
        james.execInContainer(CLI, "-h", "127.0.0.1", "-p", "9999",  "AddDomain" , SIMPSON);
        james.execInContainer(CLI, "-h", "127.0.0.1", "-p", "9999",  "AddUser", BART, BART_PASSWORD);
        james.execInContainer(CLI, "-h", "127.0.0.1", "-p", "9999",  "AddUser", HOMER, HOMER_PASSWORD);

        sendMessageFromBartToHomer(james.getMappedPort(SMTP_PORT));
        assertImapMessageReceived(james.getMappedPort(IMAP_PORT));
    }
}
