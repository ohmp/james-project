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

package org.apache.james.domainlist.memory;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.james.core.Domain;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.lib.DomainListDAO;

import com.google.common.collect.ImmutableList;

public class MemoryDomainListDAO implements DomainListDAO {
    private final ConcurrentLinkedDeque<Domain> domains;

    MemoryDomainListDAO() {
        this.domains = new ConcurrentLinkedDeque<>();
    }

    @Override
    public List<Domain> getDomainListInternal() {
        return ImmutableList.copyOf(domains);
    }

    @Override
    public boolean containsDomainInternal(Domain domain) {
        return domains.contains(domain);
    }

    @Override
    public void addDomain(Domain domain) throws DomainListException {
        boolean applied = domains.add(domain);

        if (!applied) {
            throw new DomainListException(domain.name() + " already exists.");
        }
    }

    @Override
    public void removeDomain(Domain domain) throws DomainListException {
        if (!domains.remove(domain)) {
            throw new DomainListException(domain.name() + " was not found");
        }
    }
}
