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

package org.apache.james.mailbox.elasticsearch.tasks;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.apache.james.backends.es.AliasName;
import org.apache.james.backends.es.IndexCreationFactory;
import org.apache.james.backends.es.IndexName;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class IndexCreationConfiguration {

    public static class Builder {
        private IndexName indexName;
        private Optional<Integer> schemaVersion;
        private Optional<Integer> nbShards;
        private Optional<Integer> nbReplica;
        private ImmutableList.Builder<AliasName> aliases;

        public Builder() {
            aliases = ImmutableList.builder();
        }

        public Builder indexName(IndexName indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = Optional.of(schemaVersion);
            return this;
        }

        public Builder nbShards(int nbShards) {
            this.nbShards = Optional.of(nbShards);
            return this;
        }

        public Builder nbReplica(int nbReplica) {
            this.nbReplica = Optional.of(nbReplica);
            return this;
        }

        public Builder schemaVersion(Optional<Integer> schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder nbShards(Optional<Integer> nbShards) {
            this.nbShards = nbShards;
            return this;
        }

        public Builder nbReplica(Optional<Integer> nbReplica) {
            this.nbReplica = nbReplica;
            return this;
        }

        public Builder addAliases(Collection<AliasName> aliasNames) {
            this.aliases.addAll(aliasNames);
            return this;
        }

        public IndexCreationConfiguration build() {
            Preconditions.checkNotNull(indexName);

            return new IndexCreationConfiguration(schemaVersion.orElse(1),
                indexName,
                nbShards.orElse(IndexCreationFactory.DEFAULT_NB_SHARDS),
                nbReplica.orElse(IndexCreationFactory.DEFAULT_NB_REPLICA),
                aliases.build());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final IndexName indexName;
    private final int schemaVersion;
    private final int nbShards;
    private final int nbReplica;
    private final ImmutableList<AliasName> aliasNames;

    private IndexCreationConfiguration(int schemaVersion, IndexName indexName, int nbShards, int nbReplica, ImmutableList<AliasName> aliasNames) {
        this.schemaVersion = schemaVersion;
        this.indexName = indexName;
        this.nbShards = nbShards;
        this.nbReplica = nbReplica;
        this.aliasNames = aliasNames;
    }


    public int getSchemaVersion() {
        return schemaVersion;
    }

    public IndexName getIndexName() {
        return indexName;
    }

    public int getNbShards() {
        return nbShards;
    }

    public int getNbReplica() {
        return nbReplica;
    }

    public ImmutableList<AliasName> getAliasNames() {
        return aliasNames;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof IndexCreationConfiguration) {
            IndexCreationConfiguration that = (IndexCreationConfiguration) o;

            return Objects.equals(this.schemaVersion, that.schemaVersion)
                && Objects.equals(this.nbShards, that.nbShards)
                && Objects.equals(this.nbReplica, that.nbReplica)
                && Objects.equals(this.indexName, that.indexName)
                && Objects.equals(this.aliasNames, that.aliasNames);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(schemaVersion, indexName, nbShards, nbReplica, aliasNames);
    }
}
