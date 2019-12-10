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
package org.apache.james.imap.api.message;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

public class FetchData {
    public static class Builder {
        private EnumSet<Item> itemToFetch = EnumSet.noneOf(Item.class);
        private Set<BodyFetchElement> bodyElements = new HashSet<>();
        private boolean setSeen = false;
        private long changedSince = -1;
        private boolean vanished;

        public Builder from(FetchData fetchData) {
            itemToFetch = EnumSet.copyOf(fetchData.itemToFetch);
            setSeen = fetchData.setSeen;
            changedSince = fetchData.changedSince;
            vanished = fetchData.vanished;
            bodyElements = new HashSet<>();
            bodyElements.addAll(fetchData.bodyElements);
            return this;
        }

        public Builder fetch(Item item) {
            itemToFetch.add(item);
            return this;
        }

        public Builder setChangedSince(long changedSince) {
            this.changedSince = changedSince;
            itemToFetch.add(Item.MODSEQ);
            return this;
        }

        /**
         * Set to true if the VANISHED FETCH modifier was used as stated in <code>QRESYNC</code> extension
         */
        public Builder setVanished(boolean vanished) {
            this.vanished = vanished;
            return this;
        }

        public Builder add(BodyFetchElement element, boolean peek) {
            if (!peek) {
                setSeen = true;
            }
            bodyElements.add(element);
            return this;
        }

        public FetchData build() {
            return new FetchData(itemToFetch, bodyElements, setSeen, changedSince, vanished);
        }
    }

    public enum Item {
        FLAGS,
        UID,
        INTERNAL_DATE,
        SIZE,
        ENVELOPE,
        BODY,
        BODY_STRUCTURE,
        MODSEQ,
    }

    public static Builder builder() {
        return new Builder();
    }

    private final EnumSet<Item> itemToFetch;
    private final Set<BodyFetchElement> bodyElements;
    private final boolean setSeen;
    private final long changedSince;
    private final boolean vanished;

    private FetchData(EnumSet<Item> itemToFetch, Set<BodyFetchElement> bodyElements, boolean setSeen, long changedSince, boolean vanished) {
        this.itemToFetch = EnumSet.copyOf(itemToFetch);
        this.bodyElements = ImmutableSet.copyOf(bodyElements);
        this.setSeen = setSeen;
        this.changedSince = changedSince;
        this.vanished = vanished;
    }

    public Collection<BodyFetchElement> getBodyElements() {
        return bodyElements;
    }

    public boolean contains(Item item) {
        return itemToFetch.contains(item);
    }

    public boolean isSetSeen() {
        return setSeen;
    }
    
    public long getChangedSince() {
        return changedSince;
    }
    
    /**
     * Return true if the VANISHED FETCH modifier was used as stated in <code>QRESYNC<code> extension
     */
    public boolean getVanished() {
        return vanished;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(itemToFetch, bodyElements, setSeen, changedSince);
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof FetchData) {
            FetchData fetchData = (FetchData) o;

            return Objects.equals(this.setSeen, fetchData.setSeen)
                && Objects.equals(this.changedSince, fetchData.changedSince)
                && Objects.equals(this.itemToFetch, fetchData.itemToFetch)
                && Objects.equals(this.bodyElements, fetchData.bodyElements);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("flags", contains(Item.FLAGS))
            .add("uid", contains(Item.UID))
            .add("internalDate", contains(Item.INTERNAL_DATE))
            .add("size", contains(Item.SIZE))
            .add("envelope", contains(Item.ENVELOPE))
            .add("body", contains(Item.BODY))
            .add("bodyStructure", contains(Item.BODY_STRUCTURE))
            .add("setSeen", setSeen)
            .add("bodyElements", ImmutableSet.copyOf(bodyElements))
            .add("modSeq", contains(Item.MODSEQ))
            .add("changedSince", changedSince)
            .add("vanished", vanished)
            .toString();
    }
}
