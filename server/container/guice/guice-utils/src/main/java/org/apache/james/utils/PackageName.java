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

package org.apache.james.utils;

import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public class PackageName {
    static final char PART_SEPARATOR = '.';

    public static PackageName of(String value) {
        Preconditions.checkNotNull(value);

        Iterable<String> packageParts = Splitter.on(PART_SEPARATOR)
            .omitEmptyStrings()
            .split(value);

        return new PackageName(Joiner.on(PART_SEPARATOR)
            .join(packageParts));
    }

    private final String name;

    private PackageName(String name) {
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.isEmpty(), "Name should not be empty");

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof PackageName) {
            PackageName className = (PackageName) o;

            return Objects.equals(this.name, className.name);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name);
    }
}
