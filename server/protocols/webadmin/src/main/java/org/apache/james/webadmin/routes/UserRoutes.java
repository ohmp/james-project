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

import static org.apache.james.webadmin.Constants.SEPARATOR;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.post;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.User;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.model.AddUserRequest;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import spark.Request;
import spark.Response;

public class UserRoutes implements Routes {

    private static final String USER_NAME = ":userName";
    private static final String EMPTY_BODY = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoutes.class);

    public static final String USERS = "/users";

    private final UsersRepository usersRepository;
    private final JsonTransformer jsonTransformer;
    private final JsonExtractor<AddUserRequest> jsonExtractor;

    @Inject
    public UserRoutes(UsersRepository usersRepository, JsonTransformer jsonTransformer) {
        this.usersRepository = usersRepository;
        this.jsonTransformer = jsonTransformer;
        this.jsonExtractor = new JsonExtractor<>(AddUserRequest.class);
    }

    @Override
    public void define() {
        get(USERS,
            (request, response) -> getUsers(),
            jsonTransformer);

        post(USERS, this::addUser);

        delete(USERS + SEPARATOR + USER_NAME, this::removeUser);
    }

    private List<String> getUsers() throws UsersRepositoryException {
        return Optional.ofNullable(usersRepository.list())
            .map(ImmutableList::copyOf)
            .orElse(ImmutableList.of());
    }

    private String removeUser(Request request, Response response) {
        String username = request.params(USER_NAME);
        try {
            usersRepository.removeUser(username);
            response.status(204);
            return EMPTY_BODY;
        } catch (UsersRepositoryException e) {
            response.status(204);
            return "The user " + username + " does not exists";
        }
    }

    private String addUser(Request request, Response response) throws IOException, UsersRepositoryException {
        try {
            AddUserRequest addUserRequest = jsonExtractor.parse(request.body());
            response.status(204);
            return addUser(addUserRequest);
        } catch (IOException e) {
            LOGGER.info("Error while deserializing addUser request", e);
            halt(400);
            return EMPTY_BODY;
        }
    }

    private String addUser(AddUserRequest addUserRequest) throws UsersRepositoryException {
        User user = usersRepository.getUserByName(addUserRequest.getUsername());
        if (user == null) {
            addUser(addUserRequest.getUsername(), addUserRequest.getPassword());
        } else {
            user.setPassword(new String(addUserRequest.getPassword()));
            usersRepository.updateUser(user);
        }
        halt(204);
        return EMPTY_BODY;
    }

    private void addUser(String username, char[] password) throws UsersRepositoryException {
        try {
            usersRepository.addUser(username, new String(password));
        } catch (UsersRepositoryException e) {
            LOGGER.info("Race condition while creating user {} : user already exists", username);
        }
    }
}
