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
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.Constants.SEPARATOR;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class UsersRoutesTest {

    public static final String DOMAIN = "domain";
    public static final String USERNAME = "username@" + DOMAIN;
    public static final String PATH_SPECIFIC_USER = UserRoutes.USERS + SEPARATOR + USERNAME;
    private WebAdminServer webAdminServer;

    private void createServer(UsersRepository usersRepository) throws Exception {
        webAdminServer = new WebAdminServer(new UserRoutes(usersRepository, new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.port = webAdminServer.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
        RestAssured.defaultParser = Parser.JSON;
    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    public class NormalBehaviour {

        @Before
        public void setUp() throws Exception {
            DomainList domainList = mock(DomainList.class);
            when(domainList.containsDomain(DOMAIN)).thenReturn(true);

            MemoryUsersRepository usersRepository = MemoryUsersRepository.withVirtualHosting();
            usersRepository.setDomainList(domainList);

            createServer(usersRepository);
        }


        @Test
        public void getUsersShouldBeEmptyByDefault() {
            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(200)
                .body(is("[]"));
        }

        @Test
        public void postShouldReturnUserErrorWhenNoBody() {
            when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnUserErrorWhenEmptyJsonBody() {
            given()
                .body("{}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnUserErrorWhenWrongJsonBody() {
            given()
                .body("{\"bad\":\"any\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnUserErrorWhenMissingPasswordJsonBody() {
            given()
                .body("{\"username\":\"username\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnUserErrorWhenMissingUserJsonBody() {
            given()
                .body("{\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnOkWhenValidJsonBody() {
            given()
                .body("{\"username\":\"username@domain\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(204);
        }

        @Test
        public void postShouldReturnRequireNonNullUsername() {
            given()
                .body("{\"username\":null,\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnRequireNotEmptyUsername() {
            given()
                .body("{\"username\":\"\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldReturnRequireNonNullPassword() {
            given()
                .body("{\"username\":\"username@domain\",\"password\":null}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(400);
        }

        @Test
        public void postShouldAddTheUser() {
            with()
                .body("{\"username\":\"" + USERNAME + "\",\"password\":\"password\"}")
            .post(UserRoutes.USERS);

            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(200)
                .body(containsString(USERNAME));
        }

        @Test
        public void postingTwoTimesShouldBeAllowed() {
            // Given
            with()
                .body("{\"username\":\"" + USERNAME + "\",\"password\":\"password\"}")
            .post(UserRoutes.USERS);

            // When
            given()
                .body("{\"username\":\"" + USERNAME + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(204);

            // Then
            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(200)
                .body(equalTo("[\"" + USERNAME + "\"]"));
        }

        @Test
        public void deleteShouldReturnOk() {
            when()
                .delete(PATH_SPECIFIC_USER)
            .then()
                .statusCode(204);
        }

        @Test
        public void deleteShouldRemoveAssociatedUser() {
            // Given
            with()
                .body("{\"username\":\"" + USERNAME + "\",\"password\":\"password\"}")
            .post(UserRoutes.USERS);

            // When
            when()
                .delete(PATH_SPECIFIC_USER)
            .then()
                .statusCode(204);

            // Then
            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(200)
                .body(equalTo("[]"));
        }

        @Test
        public void deleteShouldStillBeValidWithExtraBody() {
            given()
                .body("{\"bad\":\"any\"}")
            .when()
                .delete(PATH_SPECIFIC_USER)
            .then()
                .statusCode(204);
        }
    }

    public class ErrorHandling {

        private UsersRepository usersRepository;
        private String username;
        private String password;

        @Before
        public void setUp() throws Exception {
            usersRepository = mock(UsersRepository.class);
            createServer(usersRepository);
            username = "username@domain";
            password = "password";
        }

        @Test
        public void deleteShouldStillBeOkWhenNoUser() throws Exception {
            doThrow(new UsersRepositoryException("message")).when(usersRepository).removeUser(username);

            when()
                .delete(PATH_SPECIFIC_USER)
            .then()
                .statusCode(204);
        }

        @Test
        public void getShouldFailOnRepositoryException() throws Exception {
            when(usersRepository.list()).thenThrow(new UsersRepositoryException("message"));

            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldFailOnRepositoryExceptionOnGetUserByName() throws Exception {
            when(usersRepository.getUserByName(username)).thenThrow(new UsersRepositoryException("message"));

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldNotFailOnRepositoryExceptionOnAddUser() throws Exception {
            when(usersRepository.getUserByName(username)).thenReturn(null);
            doThrow(new UsersRepositoryException("message")).when(usersRepository).addUser(username, password);

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(204);
        }

        @Test
        public void postShouldFailOnRepositoryExceptionOnUpdateUser() throws Exception {
            when(usersRepository.getUserByName(username)).thenReturn(mock(User.class));
            doThrow(new UsersRepositoryException("message")).when(usersRepository).updateUser(any());

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }


        @Test
        public void deleteShouldFailOnUnknownException() throws Exception {
            doThrow(new RuntimeException()).when(usersRepository).removeUser(username);

            when()
                .delete(PATH_SPECIFIC_USER)
            .then()
                .statusCode(500);
        }

        @Test
        public void getShouldFailOnUnknownException() throws Exception {
            when(usersRepository.list()).thenThrow(new RuntimeException());

            when()
                .get(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldFailOnUnknownExceptionOnGetUserByName() throws Exception {
            when(usersRepository.getUserByName(username)).thenThrow(new RuntimeException());

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldFailOnUnknownExceptionOnAddUser() throws Exception {
            when(usersRepository.getUserByName(username)).thenReturn(null);
            doThrow(new RuntimeException()).when(usersRepository).addUser(username, password);

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldFailOnUnknownExceptionOnGetUpdateUser() throws Exception {
            when(usersRepository.getUserByName(username)).thenReturn(mock(User.class));
            doThrow(new RuntimeException()).when(usersRepository).updateUser(any());

            given()
                .body("{\"username\":\"" + username + "\",\"password\":\"password\"}")
            .when()
                .post(UserRoutes.USERS)
            .then()
                .statusCode(500);
        }
    }

}
