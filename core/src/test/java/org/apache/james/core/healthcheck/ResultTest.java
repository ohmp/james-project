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
package org.apache.james.core.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ResultTest {

    @Test
    public void statusShouldBeHealthyWhenHealthy() {
        Result result = Result.healthy();

        assertThat(result.getStatus()).isEqualTo(ResultStatus.HEALTHY);
    }

    @Test
    public void causeShouldBeEmptyWhenHealthy() {
        Result result = Result.healthy();

        assertThat(result.getCause()).isEmpty();
    }

    @Test
    public void isHealthyShouldBeTrueWhenHealthy() {
        Result result = Result.healthy();

        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    public void statusShouldBeUnhealthyWhenUnhealthy() {
        Result result = Result.unhealthy("cause");

        assertThat(result.getStatus()).isEqualTo(ResultStatus.UNHEALTHY);
    }

    @Test
    public void causeMayBeEmptyWhenUnhealthy() {
        Result result = Result.unhealthy(null);

        assertThat(result.getCause()).isEmpty();
    }

    @Test
    public void causeShouldBeKeptWhenNotEmpty() {
        String cause = "cause";
        Result result = Result.unhealthy(cause);

        assertThat(result.getCause()).contains(cause);
    }

    @Test
    public void isHealthyShouldBeFalseWhenUnhealthy() {
        Result result = Result.unhealthy(null);

        assertThat(result.isHealthy()).isFalse();
    }
}
