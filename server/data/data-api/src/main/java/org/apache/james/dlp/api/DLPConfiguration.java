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

package org.apache.james.dlp.api;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class DLPConfiguration {
    private final ImmutableList<DLPConfigurationItem> items;

    public DLPConfiguration(ImmutableList<DLPConfigurationItem> items) {
        Preconditions.checkNotNull(items);

        this.items = items;
    }

    public ImmutableList<DLPConfigurationItem> getItems() {
        return items;
    }

    public boolean containsDuplicates() {
        long uniqueIdCount = items.stream()
            .map(DLPConfigurationItem::getId)
            .distinct()
            .count();

        return uniqueIdCount != items.size();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof DLPConfiguration) {
            DLPConfiguration dlpConfiguration = (DLPConfiguration) o;

            return Objects.equals(this.items, dlpConfiguration.items);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("items", items)
            .toString();
    }
}
