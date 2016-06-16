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

import javax.inject.Inject;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.utils.JsonTransformer;

import spark.Request;
import spark.Response;

public class DomainRoutes implements Routes {

    private static final String DOMAIN_NAME = ":domainName";
    private static final String EMPTY_BODY = "";

    public static final String DOMAIN = "/domain/";
    public static final String DOMAINS = "/domains";


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

        get(DOMAIN + DOMAIN_NAME, this::exists);

        post(DOMAIN + DOMAIN_NAME, this::addDomain);

        delete(DOMAIN + DOMAIN_NAME, this::removeDomain);
    }

    private String removeDomain(Request request, Response response) {
        try {
            domainList.removeDomain(request.params(DOMAIN_NAME));
            return EMPTY_BODY;
        } catch (DomainListException e) {
            return request.params(DOMAIN_NAME) + " did not exists";
        }
    }

    private String addDomain(Request request, Response response) {
        try {
            domainList.addDomain(request.params(DOMAIN_NAME));
            return EMPTY_BODY;
        } catch (DomainListException e) {
            return request.params(DOMAIN_NAME) + " already exists";
        }
    }

    private String exists(Request request, Response response) throws DomainListException {
        if (!domainList.containsDomain(request.params(DOMAIN_NAME))) {
            response.status(404);
        } else {
            response.status(200);
        }
        return EMPTY_BODY;
    }
}
