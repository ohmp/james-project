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

import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class Linshare extends DockerComposeContainer<Linshare> {
    private static final String WAIT_FOR_LOG_MSG_PATTERN = ".*/linshare/webservice/rest/admin/authentication/change_password.*";
    private static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
    private static final String LINSHARE_BACKEND_SERVICE = "backend";
    private static final int LINSHARE_BACKEND_PORT = 8080;

    @SuppressWarnings("resource")
    public static Linshare create() throws Exception {
        File file = new File(ClassLoader.getSystemResource(DOCKER_COMPOSE_YML).toURI());
        return new Linshare(file)
            .withExposedService(LINSHARE_BACKEND_SERVICE,
                LINSHARE_BACKEND_PORT,
                Wait.forLogMessage(WAIT_FOR_LOG_MSG_PATTERN, 1));
    }

    public Linshare(File file) {
        super(file);
    }

    public int getPort() {
        return getServicePort(LINSHARE_BACKEND_SERVICE, LINSHARE_BACKEND_PORT);
    }

    public String getHost() {
        return getServiceHost(LINSHARE_BACKEND_SERVICE, LINSHARE_BACKEND_PORT);
    }
}
