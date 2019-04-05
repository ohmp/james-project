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
package org.apache.james.linshare;

import java.io.File;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LinshareExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final String WAIT_FOR_LOG_MSG_PATTERN = ".*/linshare/webservice/rest/admin/authentication/change_password.*";
    public static final String BASE_PATH = "/linshare/webservice/rest/user/v2";
    private static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
    public static final String LINSHARE_BACKEND_SERVICE = "backend";
    public static final int LINSHARE_BACKEND_PORT = 8080;

    private DockerComposeContainer<?> linshare;

    @Override
    @SuppressWarnings("resource")
    public void beforeAll(ExtensionContext context) throws Exception {
        File file = new File(ClassLoader.getSystemResource(DOCKER_COMPOSE_YML).toURI());
        linshare = new DockerComposeContainer<>(file)
                .withExposedService(LINSHARE_BACKEND_SERVICE,
                    LINSHARE_BACKEND_PORT,
                    Wait.forLogMessage(WAIT_FOR_LOG_MSG_PATTERN, 1));
        linshare.start();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        linshare.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == DockerComposeContainer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return linshare;
    }
}
