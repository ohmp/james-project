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

package org.apache.james.mailrepository.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.common.base.MoreObjects;

public class MailRepositoryUrl {
    public static final MailRepositoryUrl fromEncoded(String encodedUrl) throws UnsupportedEncodingException {
        return new MailRepositoryUrl(URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.displayName()));
    }

    private final String value;

    public MailRepositoryUrl(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String encodedValue() throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.displayName());
    }

    public Protocol getProtocol() {
        int protocolSeparatorPosition = value.indexOf(':');
        if (protocolSeparatorPosition == -1) {
            throw new IllegalArgumentException("Destination is malformed. Must be a valid URL: " + value);
        }
        return new Protocol(value.substring(0, protocolSeparatorPosition));
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof MailRepositoryUrl) {
            MailRepositoryUrl that = (MailRepositoryUrl) o;

            return Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("value", value)
            .toString();
    }
}
