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

package org.apache.james.metrics.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

public class UnionMetric implements Metric {

    private final ImmutableSet<Metric> underlying;

    public UnionMetric(ImmutableSet<Metric> underlying) {
        Preconditions.checkArgument(!underlying.isEmpty());
        this.underlying = underlying;
    }

    private Predicate<TimeMetric> hasDifferentName(final String name) {
        return new Predicate<TimeMetric>() {
            @Override
            public boolean apply(TimeMetric input) {
                return !input.name().equals(name);
            }
        };
    }

    @Override
    public void increment() {
        for (Metric metric : underlying) {
            metric.increment();
        }
    }

    @Override
    public void decrement() {
        for (Metric metric : underlying) {
            metric.decrement();
        }
    }
}
