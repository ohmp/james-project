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

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

public class UnionMetricFactory implements MetricFactory {

    private ImmutableSet<MetricFactory> underlying;

    @Inject
    public UnionMetricFactory(Set<MetricFactory> underlying) {
        LoggerFactory.getLogger(this.getClass()).info("Union metric factory with " + underlying);
        Preconditions.checkArgument(!underlying.isEmpty(), "Can not create UnionMetricFactory on top of no others metric factories");
        this.underlying = ImmutableSet.copyOf(underlying);
    }

    @Override
    public Metric generate(String name) {
        return new UnionMetric(FluentIterable.from(underlying)
            .transform(createMetric(name))
            .toSet());
    }

    @Override
    public TimeMetric timer(final String name) {
        return new UnionTimeMetric(FluentIterable.from(underlying)
            .transform(createTimeMetric(name))
            .toSet());
    }

    private Function<MetricFactory, Metric> createMetric(final String name) {
        return new Function<MetricFactory, Metric>() {
            @Override
            public Metric apply(MetricFactory input) {
                return input.generate(name);
            }
        };
    }

    private Function<MetricFactory, TimeMetric> createTimeMetric(final String name) {
        return new Function<MetricFactory, TimeMetric>() {
            @Override
            public TimeMetric apply(MetricFactory input) {
                return input.timer(name);
            }
        };
    }
}
