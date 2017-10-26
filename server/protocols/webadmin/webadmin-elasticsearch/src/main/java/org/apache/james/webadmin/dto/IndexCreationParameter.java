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

package org.apache.james.webadmin.dto;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class IndexCreationParameter {
    private final int schemaVersion;
    private final Optional<Integer> nbShards;
    private final Optional<Integer> nbReplica;
    private final ImmutableList<String> aliases;

    @JsonCreator
    public IndexCreationParameter(@JsonProperty("schemaVersion") int schemaVersion,
                                  @JsonProperty("nbShards") Optional<Integer> nbShards,
                                  @JsonProperty("nbReplica") Optional<Integer> nbReplica,
                                  @JsonProperty("aliases") Optional<ImmutableList<String>> aliases) {
        this.schemaVersion = schemaVersion;
        this.nbShards = nbShards;
        this.nbReplica = nbReplica;
        this.aliases = aliases.orElse(ImmutableList.of());
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Optional<Integer> getNbShards() {
        return nbShards;
    }

    public Optional<Integer> getNbReplica() {
        return nbReplica;
    }

    public ImmutableList<String> getAliases() {
        return aliases;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof IndexCreationParameter) {
            IndexCreationParameter that = (IndexCreationParameter) o;

            return Objects.equals(this.schemaVersion, that.schemaVersion)
                && Objects.equals(this.nbShards, that.nbShards)
                && Objects.equals(this.nbReplica, that.nbReplica)
                && Objects.equals(this.aliases, that.aliases);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(schemaVersion, nbShards, nbReplica, aliases);
    }
}
