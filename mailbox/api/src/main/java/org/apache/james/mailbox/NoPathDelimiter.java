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
package org.apache.james.mailbox;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

public class NoPathDelimiter implements PathDelimiter {

    public static final NoPathDelimiter SINGLETON = new NoPathDelimiter();

    private Joiner joiner;

    private NoPathDelimiter() {
        joiner = Joiner.on("");
    }

    public String join(String... paths) {
        return joiner.join(paths);
    }

    public String join(Iterable<String> paths) {
        return joiner.join(paths);
    }

    public List<String> split(String path) {
        return ImmutableList.of(path);
    }

    public String getLastPathPart(String path) {
        return path;
    }

    public String getFirstPathPart(String path) {
        return path;
    }

    public boolean containsPathDelimiter(String name) {
        return false;
    }

    public Optional<String> getParent(String name) {
        return Optional.empty();
    }

    public String appendDelimiter(String name) {
        return name;
    }

    public String removeTrailingDelimiter(String name) {
        return name;
    }

    public String removeHeadingDelimiter(String name) {
        return name;
    }

    public Stream<String> getHierarchyLevels(String name) {
        return Stream.of(name);
    }

    public boolean isUndefined() {
        return true;
    }

    public char asChar() {
        return Character.UNASSIGNED;
    }

    public String asString() {
        return "";
    }

    public String toPattern() {
        return "";
    }
    
    @Override
    public final int hashCode() {
        return 35668367;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof NoPathDelimiter;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
            .toString();
    }
}
