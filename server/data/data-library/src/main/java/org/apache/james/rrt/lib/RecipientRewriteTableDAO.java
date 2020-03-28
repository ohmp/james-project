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

package org.apache.james.rrt.lib;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.james.core.Domain;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public interface RecipientRewriteTableDAO {
    void addMapping(MappingSource source, Mapping mapping) throws RecipientRewriteTableException;

    void removeMapping(MappingSource source, Mapping mapping) throws RecipientRewriteTableException;

    Mappings getStoredMappings(MappingSource source) throws RecipientRewriteTableException;

    /**
     * Return a Map which holds all Mappings
     *
     * @return Map
     */
    Map<MappingSource, Mappings> getAllMappings() throws RecipientRewriteTableException;

    /**
     * This method must return stored Mappings for the given user.
     * It must never return null but throw RecipientRewriteTableException on errors and return an empty Mappings
     * object if no mapping is found.
     */
    Mappings mapAddress(String user, Domain domain) throws RecipientRewriteTableException;

    default Stream<MappingSource> listSources(Mapping mapping) throws RecipientRewriteTableException {
        Preconditions.checkArgument(RecipientRewriteTable.listSourcesSupportedType.contains(mapping.getType()),
            "Not supported mapping of type %s", mapping.getType());

        return getAllMappings()
            .entrySet().stream()
            .filter(entry -> entry.getValue().contains(mapping))
            .map(Map.Entry::getKey);
    }

    default Stream<MappingSource> getSourcesForType(Mapping.Type type) throws RecipientRewriteTableException {
        return getAllMappings()
            .entrySet().stream()
            .filter(e -> e.getValue().contains(type))
            .map(Map.Entry::getKey)
            .sorted(Comparator.comparing(MappingSource::asMailAddressString));
    }

    default Stream<Mapping> getMappingsForType(Mapping.Type type) throws RecipientRewriteTableException {
        return ImmutableSet.copyOf(getAllMappings()
            .values().stream()
            .map(mappings -> mappings.select(type))
            .reduce(Mappings::union)
            .orElse(MappingsImpl.empty()))
            .stream();
    }
}
