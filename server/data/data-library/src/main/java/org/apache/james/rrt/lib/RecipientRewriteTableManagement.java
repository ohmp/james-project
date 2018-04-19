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
package org.apache.james.rrt.lib;

import java.util.Map;

import javax.inject.Inject;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.core.Domain;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.api.RecipientRewriteTableManagementMBean;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

/**
 * Management for RecipientRewriteTables
 */
public class RecipientRewriteTableManagement extends StandardMBean implements RecipientRewriteTableManagementMBean {

    private final RecipientRewriteTable rrt;

    @Inject
    protected RecipientRewriteTableManagement(RecipientRewriteTable rrt) throws NotCompliantMBeanException {
        super(RecipientRewriteTableManagementMBean.class);
        this.rrt = rrt;
    }

    @Override
    public void addRegexMapping(String user, String domain, String regex) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.addRegexMapping(source, regex);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeRegexMapping(String user, String domain, String regex) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.removeRegexMapping(source, regex);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addAddressMapping(String user, String domain, String address) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.addAddressMapping(source, address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeAddressMapping(String user, String domain, String address) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.removeAddressMapping(source, address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addErrorMapping(String user, String domain, String error) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.addErrorMapping(source, error);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeErrorMapping(String user, String domain, String error) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.removeErrorMapping(source, error);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addDomainMapping(String domain, String targetDomain) {
        try {
            MappingSource source = MappingSource.fromDomain(Domain.of(domain));
            rrt.addAliasDomainMapping(source, Domain.of(targetDomain));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeDomainMapping(String domain, String targetDomain) {
        try {
            MappingSource source = MappingSource.fromDomain(Domain.of(domain));
            rrt.removeAliasDomainMapping(source, Domain.of(targetDomain));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Mappings getUserDomainMappings(String user, String domain) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            return rrt.getUserDomainMappings(source);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addMapping(String user, String domain, String mapping) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.addMapping(source, Mapping.of(mapping));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeMapping(String user, String domain, String mapping) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.removeMapping(source, Mapping.of(mapping));
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Map<MappingSource, Mappings> getAllMappings() {
        try {
            return ImmutableMap.copyOf(rrt.getAllMappings());
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addForwardMapping(String user, String domain, String address) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.addForwardMapping(source, address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeForwardMapping(String user, String domain, String address) {
        try {
            MappingSource source = MappingSource.fromUser(user, domain);
            rrt.removeForwardMapping(source, address);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void addGroupMapping(String toUser, String toDomain, String fromAddress) {
        try {
            MappingSource source = MappingSource.fromUser(toUser, toDomain);
            rrt.addGroupMapping(source, fromAddress);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void removeGroupMapping(String toUser, String toDomain, String fromAddress) {
        try {
            MappingSource source = MappingSource.fromUser(toUser, toDomain);
            rrt.removeForwardMapping(source, fromAddress);
        } catch (RecipientRewriteTableException e) {
            throw Throwables.propagate(e);
        }
    }
}
