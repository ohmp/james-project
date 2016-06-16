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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class DomainRoutesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainRoutesTest.class);
    public static final String DOMAIN = "domain";

    private WebAdminServer webAdminServer;

    private void createServer(DomainList domainList) throws Exception {
        webAdminServer = new WebAdminServer(new DomainRoutes(domainList, new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.port = webAdminServer.getPort();
        RestAssured.config = newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8));
    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    public class NormalBehaviour {

        @Before
        public void setUp() throws Exception {
            DNSService dnsService = mock(DNSService.class);
            when(dnsService.getHostName(any())).thenReturn("localhost");
            when(dnsService.getLocalHost()).thenReturn(InetAddress.getByName("localhost"));

            MemoryDomainList domainList = new MemoryDomainList();
            domainList.setDNSService(dnsService);
            domainList.setLog(LOGGER);
            domainList.setAutoDetectIP(false);
            createServer(domainList);
        }

        @Test
        public void getDomainsShouldBeEmptyByDefault() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAINS)
            .then()
                .statusCode(200)
                .body(is("[]"));
        }

        @Test
        public void postShouldBeOk() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);
        }

        @Test
        public void getDomainsShouldDisplayAddedDomains() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAINS)
            .then()
                .statusCode(200)
                .body(is("[\"domain\"]"));
        }

        @Test
        public void postShouldWorkOnTheSecondTimeForAGivenValue() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);
        }

        @Test
        public void deleteShouldRemoveTheGivenDomain() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .delete(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAINS)
            .then()
                .statusCode(200)
                .body(is("[]"));
        }

        @Test
        public void deleteShouldBeOkWhenTheDomainIsNotPresent() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .delete(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(200);
        }

        @Test
        public void getDomainShouldReturnOkWhenTheDomainIsPresent() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(200);
        }

        @Test
        public void getDomainSHouldReturnNotFoundWhenTheDomainIsAbsent() {
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAIN + DOMAIN)
            .then()
                .statusCode(404);
        }

    }

    public class ExceptionHandling {

        private DomainList domainList;
        private String domain;

        @Before
        public void setUp() throws Exception {
            domainList = mock(DomainList.class);
            createServer(domainList);
            domain = "domain";
        }

        @Test
        public void deleteShouldReturnErrorOnUnknownException() throws Exception {
            doThrow(new RuntimeException()).when(domainList).removeDomain(domain);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .delete(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(500);
        }

        @Test
        public void postShouldReturnErrorOnUnknownException() throws Exception {
            doThrow(new RuntimeException()).when(domainList).addDomain(domain);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(500);
        }

        @Test
        public void getDomainShouldReturnErrorOnUnknownException() throws Exception {
            when(domainList.containsDomain(domain)).thenThrow(new RuntimeException());

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(500);
        }

        @Test
        public void getDomainsShouldReturnErrorOnUnknownException() throws Exception {
            when(domainList.getDomains()).thenThrow(new RuntimeException());

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAINS)
            .then()
                .statusCode(500);
        }

        @Test
        public void deleteShouldReturnOkWhenDomainListException() throws Exception {
            doThrow(new DomainListException("message")).when(domainList).removeDomain(domain);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .delete(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(200);
        }

        @Test
        public void postShouldReturnOkWhenDomainListException() throws Exception {
            doThrow(new DomainListException("message")).when(domainList).addDomain(domain);

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .post(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(200);
        }

        @Test
        public void getDomainShouldReturnErrorOnDomainListException() throws Exception {
            when(domainList.containsDomain(domain)).thenThrow(new DomainListException("message"));

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAIN + "domain")
            .then()
                .statusCode(500);
        }

        @Test
        public void getDomainsShouldReturnErrorOnDomainListException() throws Exception {
            when(domainList.getDomains()).thenThrow(new DomainListException("message"));

            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
            .when()
                .get(DomainRoutes.DOMAINS)
            .then()
                .statusCode(500);
        }

    }

}
