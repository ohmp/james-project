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

package org.apache.james.webadmin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.apache.james.webadmin.model.AddUserRequest;
import org.junit.Before;
import org.junit.Test;

public class JsonExtractorTest {

    private JsonExtractor<AddUserRequest> jsonExtractor;

    @Before
    public void setUp() {
        jsonExtractor = new JsonExtractor<>(AddUserRequest.class);
    }

    @Test
    public void parseShouldThrowOnNullInput() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void parseShouldThrowOnEmptyInput() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldThrowOnBrokenJson() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("{\"any\":\"broken")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldThrowOnEmptyJson() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("{}")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldThrowOnMissingMandatoryField() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("{\"username\":\"any\"}")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldThrowOnValidationProblem() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("{\"username\":\"\",\"password\":\"any\"}")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldThrowOnExtraFiled() throws Exception {
        assertThatThrownBy(() -> jsonExtractor.parse("{\"username\":\"\",\"password\":\"any\",\"extra\":\"extra\"}")).isInstanceOf(IOException.class);
    }

    @Test
    public void parseShouldInstantiateDestinationClass() throws Exception {
        String username = "username";
        String password = "password";
        AddUserRequest addUserRequest = jsonExtractor.parse("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");

        assertThat(addUserRequest.getUsername()).isEqualTo(username);
        assertThat(addUserRequest.getPassword()).isEqualTo(password.toCharArray());
    }

}
