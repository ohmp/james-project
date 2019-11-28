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

package org.apache.james.webadmin.data.jmap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.james.core.Username;
import org.apache.james.server.task.json.JsonTaskSerializer;
import org.apache.james.task.Task;
import org.apache.james.util.ClassLoaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecomputeUserPreviewsTaskSerializationTest {
    private static final Username BOB = Username.of("bob");

    private JsonTaskSerializer testee;
    private MessagePreviewCorrector corrector;

    @BeforeEach
    void setUp() {
        corrector = mock(MessagePreviewCorrector.class);
        testee = JsonTaskSerializer.of(RecomputeUserPreviewsTask.module(corrector));
    }

    @Test
    void serializeShouldReturnTheExpectedJson() throws Exception {
        assertThatJson(testee.serialize(new RecomputeUserPreviewsTask(BOB, corrector)))
            .isEqualTo(ClassLoaderUtils.getSystemResourceAsString("json/recomputeUser.task.json"));
    }

    @Test
    void deserializeShouldReturnTheExpectedDomainObject() throws Exception {
        Task task = testee.deserialize(ClassLoaderUtils.getSystemResourceAsString("json/recomputeUser.task.json"));
        assertThat(task).isInstanceOf(RecomputeUserPreviewsTask.class);

        RecomputeUserPreviewsTask.AdditionalInformation details = (RecomputeUserPreviewsTask.AdditionalInformation) task.details().get();
        assertThat(details.getUsername()).isEqualTo(BOB.asString());
    }
}