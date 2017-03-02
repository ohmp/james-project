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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableSet;

public class UnionMetricTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void constructorShouldThrowOnNullSet() {
        expectedException.expect(NullPointerException.class);

        new UnionMetric(null);
    }

    @Test
    public void constructorShouldThrowOnEmptySet() {
        expectedException.expect(IllegalArgumentException.class);

        new UnionMetric(ImmutableSet.<Metric>of());
    }

    @Test
    public void incrementShouldUnion() {
        Metric metric1 = mock(Metric.class);
        Metric metric2 = mock(Metric.class);

        new UnionMetric(ImmutableSet.of(metric1, metric2)).increment();

        verify(metric1).increment();
        verify(metric2).increment();
    }

    @Test
    public void decrementShouldUnion() {
        Metric metric1 = mock(Metric.class);
        Metric metric2 = mock(Metric.class);

        new UnionMetric(ImmutableSet.of(metric1, metric2)).decrement();

        verify(metric1).decrement();
        verify(metric2).decrement();
    }
}
