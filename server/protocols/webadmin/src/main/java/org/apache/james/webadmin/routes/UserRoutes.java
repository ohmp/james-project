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

import static spark.Spark.delete;
import static spark.Spark.get;
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
import org.apache.james.webadmin.utils.JsonTransformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import spark.Request;
import spark.Response;

public class UserRoutes implements Routes {

    private static final String USER_NAME = ":userName";
    private static final String EMPTY_BODY = "";

    public static final String USERS = "/users";
    public static final String USER = "/user/";

    private final UsersRepository usersRepository;
    private final JsonTransformer jsonTransformer;
    private final ObjectMapper objectMapper;

    @Inject
    public UserRoutes(UsersRepository usersRepository, JsonTransformer jsonTransformer) {
        this.usersRepository = usersRepository;
        this.jsonTransformer = jsonTransformer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void define() {
        get(USERS,
            (request, response) -> getUsers(),
            jsonTransformer);

        post(USERS, this::addUser);

        delete(USER + USER_NAME, this::removeUser);
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
            return EMPTY_BODY;
        } catch (UsersRepositoryException e) {
            return "The user " + username + " do not exists";
        }
    }

    private String addUser(Request request, Response response) throws IOException, UsersRepositoryException {
        try {
            AddUserRequest addUserRequest = objectMapper.readValue(request.body(), AddUserRequest.class);
            return addUser(addUserRequest);
        } catch (IOException e) {
            response.status(400);
            return EMPTY_BODY;
        }
    }

    private String addUser(AddUserRequest addUserRequest) throws UsersRepositoryException {
        User user = usersRepository.getUserByName(addUserRequest.getUsername());
        if (user == null) {
            usersRepository.addUser(addUserRequest.getUsername(), new String(addUserRequest.getPassword()));
        } else {
            user.setPassword(new String(addUserRequest.getPassword()));
            usersRepository.updateUser(user);
        }
        return EMPTY_BODY;
    }
}
