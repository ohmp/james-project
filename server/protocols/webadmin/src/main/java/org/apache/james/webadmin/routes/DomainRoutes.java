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
import static spark.Spark.put;

import javax.inject.Inject;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import spark.Request;
import spark.Response;

public class DomainRoutes implements Routes {

    private static final String DOMAIN_NAME = ":domainName";
    private static final String EMPTY_BODY = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainRoutes.class);

    public static final String DOMAINS = "/domains";
    public static final String SPECIFIC_DOMAIN = DOMAINS + SEPARATOR + DOMAIN_NAME;


    private final DomainList domainList;
    private final JsonTransformer jsonTransformer;

    @Inject
    public DomainRoutes(DomainList domainList, JsonTransformer jsonTransformer) {
        this.domainList = domainList;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define() {
        get(DOMAINS,
            (request, response) -> domainList.getDomains(),
            jsonTransformer);

        get(SPECIFIC_DOMAIN, this::exists);

        put(SPECIFIC_DOMAIN, this::addDomain);

        delete(SPECIFIC_DOMAIN, this::removeDomain);
    }

    private String removeDomain(Request request, Response response) {
        try {
            String domain = request.params(DOMAIN_NAME);
            removeDomain(domain);
        } catch (DomainListException e) {
            LOGGER.info("{} did not exists", request.params(DOMAIN_NAME));
        }
        response.status(204);
        return EMPTY_BODY;
    }

    private void removeDomain(String domain) throws DomainListException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(domain));
        domainList.removeDomain(domain);
    }

    private String addDomain(Request request, Response response) {
        try {
            addDomain(request.params(DOMAIN_NAME));
        } catch (DomainListException e) {
            LOGGER.info("{} already exists", request.params(DOMAIN_NAME));
        }
        response.status(204);
        return EMPTY_BODY;
    }

    private void addDomain(String domain) throws DomainListException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(domain));
        domainList.addDomain(domain);
    }

    private String exists(Request request, Response response) throws DomainListException {
        if (!domainList.containsDomain(request.params(DOMAIN_NAME))) {
            halt(404);
        } else {
            halt(204);
        }
        return EMPTY_BODY;
    }
}
