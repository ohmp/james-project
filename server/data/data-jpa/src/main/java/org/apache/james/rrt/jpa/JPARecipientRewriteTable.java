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
package org.apache.james.rrt.jpa;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.rrt.lib.RecipientRewriteTableImpl;

/**
 * Class responsible to implement the Virtual User Table in database with JPA
 * access.
 */
public class JPARecipientRewriteTable extends RecipientRewriteTableImpl<JPARecipientRewriteTableDAO> {
    @Inject
    public JPARecipientRewriteTable(DomainList domainList) {
        super(new JPARecipientRewriteTableDAO(), domainList);
    }

    /**
     * Set the entity manager to use.
     */
    @Inject
    @PersistenceUnit(unitName = "James")
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        rrtDAO.setEntityManagerFactory(entityManagerFactory);
    }
}
