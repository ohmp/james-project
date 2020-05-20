/** **************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 * ***************************************************************/

package org.apache.james.jmap.http

import java.nio.charset.StandardCharsets
import java.util.Base64

import eu.timepit.refined.auto._
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UserCredentialParserTest {
  @Test
  def shouldReturnCredentialsWhenUsernamePasswordToken(): Unit = {
    val token: String = "Basic " + toBase64("user1:password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(Some(UserCredential("user1", "password")))
  }

  @Test
  def shouldReturnCredentialsWhenRandomSpecialCharacterInUsernameToken(): Unit = {
    val token: String = "Basic " + toBase64("fd2*#jk:password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(Some(UserCredential("fd2*#jk", "password")))
  }

  @Test
  def shouldReturnCredentialsWhenRandomSpecialCharacterInBothUsernamePasswoedToken(): Unit = {
    val token: String = "Basic " + toBase64("fd2*#jk:password@fd23*&^$%")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(Some(UserCredential("fd2*#jk", "password@fd23*&^$%")))
  }

  @Test
  def shouldReturnCredentialsWhenUsernameDomainPasswordToken(): Unit = {
    val token: String = "Basic " + toBase64("user1@domain.tld:password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(Some(UserCredential("user1@domain.tld", "password")))
  }

  @Test
  def shouldReturnNoneWhenPayloadIsNotBase64(): Unit = {
    val token: String = "Basic user1:password"

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  @Test
  def shouldThrowWhenInvalidToken(): Unit = {
    val token: String = "Basic " + toBase64("user1@password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  @Test
  def shouldReturnEmptyWhenWithEmptyToken(): Unit = {
    assertThat(UserCredential.parseUserCredentials(""))
      .isEqualTo(None)
  }

  @Test
  def shouldThrowWhenWithWrongFormatToken(): Unit = {
    val token: String = "Basic " + toBase64("user1@password")

    assertThat(UserCredential.parseUserCredentials(token))
       .isEqualTo(None)
  }

  @Test
  def shouldReturnEmptyWhenWithUpperCaseToken(): Unit = {
    val token: String = "BASIC " + toBase64("user1@password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  @Test
  def shouldReturnEmptyWhenWithLowerCaseToken(): Unit = {
    val token: String = "basic " + toBase64("user1:password")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  @Test
  def shouldReturnEmptyWhenNoPassword(): Unit = {
    val token: String = "Basic " + toBase64("user1:")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  @Test
  def shouldReturnEmptyWhenNoUsername(): Unit = {
    val token: String = "Basic " + toBase64(":pass")

    assertThat(UserCredential.parseUserCredentials(token))
      .isEqualTo(None)
  }

  private def toBase64(stringValue: String): String = {
    Base64.getEncoder.encodeToString(stringValue.getBytes(StandardCharsets.UTF_8))
  }
}