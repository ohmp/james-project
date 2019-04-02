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

package org.apache.james.filesystem.api;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;

public class FileUrl {

    public enum Protocol {
        FILE("file"),
        CLASSPATH("classpath");

        static Optional<Protocol> extractProtocol(String urlAsString) {
            return Arrays.stream(values())
                .filter(protocol -> urlAsString.startsWith(protocol.getValue() + ":"))
                .findAny();
        }

        private final String value;

        Protocol(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Type {
        RELATIVE_FILE,
        ABSOLUTE_FILE,
        CONFIGURATION_FILE,

    }

    public static FileUrl of(String value) {
        Preconditions.checkNotNull(value);

        return new FileUrl(value);
    }

    public static FileUrl of(Protocol protocol, String resourceLocation) {
        return of(protocol.value + "://" + resourceLocation);
    }

    public static FileUrl fileConfiguration(String resourceLocation) {
        return relativeFile("conf/" + resourceLocation);
    }

    public static FileUrl absoluteFile(String resourceLocation) {
        return of(Protocol.FILE.value + ":" + resourceLocation);
    }

    public static FileUrl fromClasspath(String resourceLocation) {
        return of(Protocol.CLASSPATH, resourceLocation);
    }

    public static FileUrl relativeFile(String resourceLocation) {
        return of(Protocol.FILE, resourceLocation);
    }

    private final String value;

    public FileUrl(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean hasProtocol(Protocol protocol) {
        return value.startsWith(protocol.value + ":");
    }

    public FileUrl append(String urlPart) {
        return FileUrl.of(value + "/" + urlPart);
    }

    public String toRelativeFilePath() {
        Preconditions.checkState(value.startsWith(Protocol.FILE.value + "://"));

        return value.substring(Protocol.FILE.value.length() + 3);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FileUrl) {
            FileUrl fileUrl = (FileUrl) o;

            return Objects.equals(this.value, fileUrl.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }
}
