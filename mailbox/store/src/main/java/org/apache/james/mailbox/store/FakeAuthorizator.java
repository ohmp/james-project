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
package org.apache.james.mailbox.store;

import java.util.Optional;

import org.apache.james.core.User;

public class FakeAuthorizator implements Authorizator {

    public static FakeAuthorizator defaultReject() {
        return new FakeAuthorizator(Optional.empty(), Optional.empty());
    }

    public static FakeAuthorizator forUserAndAdmin(User admin, User user) {
        return new FakeAuthorizator(Optional.of(admin), Optional.of(user));
    }

    private final Optional<User> adminId;
    private final Optional<User> delegatedUserId;

    private FakeAuthorizator(Optional<User> adminId, Optional<User> userId) {
        this.adminId = adminId;
        this.delegatedUserId = userId;
    }

    @Override
    public AuthorizationState canLoginAsOtherUser(User user, User otherUser) {
        if (!adminId.isPresent() || !this.delegatedUserId.isPresent()) {
            return AuthorizationState.NOT_ADMIN;
        }
        if (!adminId.get().equals(user)) {
            return AuthorizationState.NOT_ADMIN;
        }
        if (!otherUser.equals(this.delegatedUserId.get())) {
            return AuthorizationState.UNKNOWN_USER;
        }
        return AuthorizationState.ALLOWED;
    }
}

