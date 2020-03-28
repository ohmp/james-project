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

package org.apache.james.domainlist.xml;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.apache.james.core.Domain;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.lib.DomainListDAO;

/**
 * Mimic the old behavior of JAMES
 */
@Singleton
public class XMLDomainListDAO implements DomainListDAO {
    private final List<Domain> domainNames = new ArrayList<>();
    private boolean modifiable = true;

    public void markAsUnmodifiable() {
        modifiable = false;
    }

    @Override
    public List<Domain> getDomainListInternal() {
        return new ArrayList<>(domainNames);
    }

    @Override
    public boolean containsDomainInternal(Domain domain) throws DomainListException {
        return domainNames.contains(domain);
    }

    @Override
    public void addDomain(Domain domain) throws DomainListException {
        if (!modifiable) {
            throw new DomainListException("Read-Only DomainList implementation");
        }
        domainNames.add(domain);
    }

    @Override
    public void removeDomain(Domain domain) throws DomainListException {
        if (!modifiable) {
            throw new DomainListException("Read-Only DomainList implementation");
        }
        domainNames.remove(domain);
    }

}
