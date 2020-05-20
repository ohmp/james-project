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

package org.apache.james.jmap.rfc8621.contract

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.netty.handler.codec.http.HttpHeaderNames.ACCEPT
import io.restassured.RestAssured._
import io.restassured.http.{Header, Headers}
import org.apache.http.HttpStatus
import org.apache.james.GuiceJamesServer
import org.apache.james.jmap.rfc8621.contract.AuthenticationContract._
import org.apache.james.jmap.rfc8621.contract.Fixture.{BOB, BOB_PASSWORD, DOMAIN, baseRequestSpecBuilder}
import org.apache.james.utils.DataProbeImpl
import org.junit.jupiter.api.{BeforeEach, Test}

object AuthenticationContract {
  private val AUTHORIZATION_HEADER: String = "Authorization"
}

trait AuthenticationContract {
  @BeforeEach
  def setUp(server: GuiceJamesServer): Unit = {
    server.getProbe(classOf[DataProbeImpl])
      .fluent()
      .addDomain(DOMAIN.asString())
      .addUser(BOB.asString(), BOB_PASSWORD)

    requestSpecification = baseRequestSpecBuilder(server).build
  }

  @Test
  def postShouldRespondUnauthorizedWhenNoCredentials(): Unit = {
    `given`()
      .header(ACCEPT.toString, Fixture.ACCEPT_RFC8621_VERSION_HEADER)
      .body(Fixture.ECHO_REQUEST_OBJECT)
    .when()
      .post()
    .then
      .statusCode(HttpStatus.SC_UNAUTHORIZED)
  }

  @Test
  def postShouldRespond200WhenHadCredentials(): Unit = {
    val authHeader: Header = new Header(AUTHORIZATION_HEADER, s"Basic ${toBase64("bob@domain.tld:bobpassword")}")
    `given`()
      .headers(getHeadersWith(authHeader))
      .body(Fixture.ECHO_REQUEST_OBJECT)
    .when()
      .post()
    .then
      .statusCode(HttpStatus.SC_OK)
  }

  @Test
  def postShouldRespond401WhenCredentialsWithWrongDomain(): Unit = {
    val authHeader: Header = new Header(AUTHORIZATION_HEADER, s"Basic ${toBase64("bob@@domain.tld:bobpassword")}")
    `given`()
      .headers(getHeadersWith(authHeader))
      .body(Fixture.ECHO_REQUEST_OBJECT)
    .when()
      .post()
    .then
      .statusCode(HttpStatus.SC_UNAUTHORIZED)
  }

  @Test
  def postShouldRespond401WhenCredentialsWith2DotDomain(): Unit = {
    val authHeader: Header = new Header(AUTHORIZATION_HEADER, s"Basic ${toBase64("bob@do.main.tld:bobpassword")}")
    `given`()
      .headers(getHeadersWith(authHeader))
      .body(Fixture.ECHO_REQUEST_OBJECT)
    .when()
      .post()
    .then
      .statusCode(HttpStatus.SC_UNAUTHORIZED)
  }

  @Test
  def postShouldRespond401WhenCredentialsWithSpaceDomain(): Unit = {
    val authHeader: Header = new Header(AUTHORIZATION_HEADER, s"Basic ${toBase64("bob@do main.tld:bobpassword")}")
    `given`()
      .headers(getHeadersWith(authHeader))
      .body(Fixture.ECHO_REQUEST_OBJECT)
    .when()
      .post()
    .then
      .statusCode(HttpStatus.SC_UNAUTHORIZED)
  }

  private def getHeadersWith(authHeader: Header): Headers = {
    new Headers(
      new Header(ACCEPT.toString, Fixture.ACCEPT_RFC8621_VERSION_HEADER),
      authHeader
    )
  }

  private def toBase64(stringValue: String): String = {
    Base64.getEncoder.encodeToString(stringValue.getBytes(StandardCharsets.UTF_8))
  }
}
