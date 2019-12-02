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

package org.apache.james.mailbox.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class BlobTest {
    private static final BlobId ID = BlobId.fromString("123");
    private static final String CONTENT_TYPE = "text/plain";
    private static final byte[] PAYLOAD = "abc".getBytes(StandardCharsets.UTF_8);
    private static final Supplier<InputStream> PAYLOAD_SUPPLIER = () -> new ByteArrayInputStream(PAYLOAD);

    @Test
    void shouldMatchBeanContract() {
        EqualsVerifier.forClass(Blob.class)
            .withIgnoredFields("payload", "size")
            .verify();
    }

    @Test
    void buildShouldConstructValidBlob() {
        assertThat(
            Blob.builder()
                .id(ID)
                .contentType(CONTENT_TYPE)
                .payload(PAYLOAD)
                .build())
            .isEqualTo(
                new Blob(ID, PAYLOAD_SUPPLIER, CONTENT_TYPE, Long.valueOf(PAYLOAD.length)));
    }

    @Test
    void buildShouldThrowOnMissingBlobId() {
        assertThatThrownBy(() ->
            Blob.builder()
                .contentType(CONTENT_TYPE)
                .payload(PAYLOAD)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildShouldThrowOnMissingContentType() {
        assertThatThrownBy(() ->
            Blob.builder()
                .id(ID)
                .payload(PAYLOAD)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildShouldThrowOnMissingPayload() {
        assertThatThrownBy(() ->
            Blob.builder()
                .id(ID)
                .contentType(CONTENT_TYPE)
                .size(36)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildShouldThrowOnMissingSize() {
        assertThatThrownBy(() ->
            Blob.builder()
                .id(ID)
                .contentType(CONTENT_TYPE)
                .payload(PAYLOAD_SUPPLIER)
                .build())
            .isInstanceOf(IllegalStateException.class);
    }
}
