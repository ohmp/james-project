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

import java.time.Instant;

import org.apache.james.server.task.json.JsonTaskAdditionalInformationSerializer;
import org.apache.james.util.ClassLoaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecomputeAllPreviewsTaskAdditionalInformationDTOTest {
    private static final Instant INSTANT = Instant.parse("2007-12-03T10:15:30.00Z");
    private static final RecomputeAllPreviewsTaskAdditionalInformationDTO DTO = new RecomputeAllPreviewsTaskAdditionalInformationDTO("RecomputeAllPreviewsTask", INSTANT, 1, 2, 3, 4);
    private static final RecomputeAllPreviewsTask.AdditionalInformation DOMAIN_OBJECT = new RecomputeAllPreviewsTask.AdditionalInformation(1, 2, 3, 4, INSTANT);

    private JsonTaskAdditionalInformationSerializer testee;

    @BeforeEach
    void setUp() {
        testee = JsonTaskAdditionalInformationSerializer.of(RecomputeAllPreviewsTaskAdditionalInformationDTO.SERIALIZATION_MODULE);
    }

    @Test
    void toDTOShouldReturnCorrectObject() {
        RecomputeAllPreviewsTaskAdditionalInformationDTO actual = RecomputeAllPreviewsTaskAdditionalInformationDTO.SERIALIZATION_MODULE
            .toDTO(DOMAIN_OBJECT);

        assertThat(actual).isEqualToComparingFieldByField(DTO);
    }

    @Test
    void toDomainObjectShouldReturnCorrectObject() {
        RecomputeAllPreviewsTask.AdditionalInformation actual = RecomputeAllPreviewsTaskAdditionalInformationDTO.SERIALIZATION_MODULE
            .getToDomainObjectConverter()
            .convert(DTO);

        assertThat(actual).isEqualToComparingFieldByField(DOMAIN_OBJECT);
    }

    @Test
    void serializeShouldReturnExpectedString() throws Exception {
        assertThatJson(testee.serialize(DOMAIN_OBJECT))
            .isEqualTo(ClassLoaderUtils.getSystemResourceAsString("json/recomputeAll.additionalInformation.json"));
    }

    @Test
    void deserializeShouldReturnExpectedDomainObject() throws Exception {
        assertThat(testee.deserialize(ClassLoaderUtils.getSystemResourceAsString("json/recomputeAll.additionalInformation.json")))
            .isEqualToComparingFieldByField(DOMAIN_OBJECT);
    }
}