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
package org.apache.james.domainlist.jpa;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.lib.DomainListImpl;

/**
 * JPA implementation of the DomainList.<br>
 * This implementation is compatible with the JDBCDomainList, meaning same
 * database schema can be reused.
 */
public class JPADomainList extends DomainListImpl<JPADomainListDAO> {
    @Inject
    JPADomainList(DNSService dns, EntityManagerFactory entityManagerFactory) {
        super(dns, new JPADomainListDAO(entityManagerFactory));
    }

    /**
     * Set the entity manager to use.
     */
    @Inject
    @PersistenceUnit(unitName = "James")
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        domainListDAO.setEntityManagerFactory(entityManagerFactory);
    }

    @PostConstruct
    public void init() {
        domainListDAO.init();
    }
}
