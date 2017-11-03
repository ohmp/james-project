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

package org.apache.james.user.lib;

import java.util.List;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.user.api.UsersRepositoryException;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;

public class UsernameValidator {

    private final boolean supportsVirtualHosting;
    private final DomainList domainList;
    private final String forbiddenCharactersInLocalParts;

    public UsernameValidator(boolean supportsVirtualHosting, DomainList domainList, String forbiddenCharactersInLocalParts) {
        this.supportsVirtualHosting = supportsVirtualHosting;
        this.domainList = domainList;
        this.forbiddenCharactersInLocalParts = forbiddenCharactersInLocalParts;
    }

    public void validate(String username) throws UsersRepositoryException {
        Preconditions.checkNotNull(username);
        int i = username.indexOf("@");
        if (supportsVirtualHosting) {
            // need a @ in the username
            if (i == -1) {
                throw new UsersRepositoryException("Given Username needs to contain a @domainpart");
            } else {
                validateUserDomain(username.substring(i + 1));
                validateUserLocalPart(username.substring(0, i), username);
            }
        } else {
            // @ only allowed when virtualhosting is supported
            if (i != -1) {
                throw new UsersRepositoryException("Given Username contains a @domainpart but virtualhosting support is disabled");
            }
            validateUserLocalPart(username, username);
        }
    }

    private void validateUserDomain(String domain) throws UsersRepositoryException {
        try {
            if (!domainList.containsDomain(domain)) {
                throw new UsersRepositoryException("Domain does not exist in DomainList");
            }
        } catch (DomainListException e) {
            throw new UsersRepositoryException("Unable to query DomainList", e);
        }
    }

    private void validateUserLocalPart(String localPart, String username) throws UsersRepositoryException {
        if (localPart.isEmpty()) {
            throw new UsersRepositoryException(username + " has an empty local part");
        }
        List<Character> forbiddenChars = forbiddenCharactersInLocalParts.chars()
            .mapToObj(i -> (char) i)
            .filter(c -> localPart.indexOf(c) >= 0)
            .collect(Guavate.toImmutableList());
        if (!forbiddenChars.isEmpty()) {
            throw new UsersRepositoryException(username + " contains forbidden characters " + forbiddenChars);
        }
    }
}
