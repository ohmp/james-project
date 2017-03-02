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
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableSet;

public class UnionTimeMetricTest {

    public static final String NAME = "name";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void constructorShouldThrowOnNullSet() {
        expectedException.expect(NullPointerException.class);

        new UnionTimeMetric(null);
    }

    @Test
    public void constructorShouldThrowOnEmptySet() {
        expectedException.expect(IllegalArgumentException.class);

        new UnionTimeMetric(ImmutableSet.<TimeMetric>of());
    }

    @Test
    public void constructorShouldThrowOnDifferentNames() {
        TimeMetric timeMetric1 = mock(TimeMetric.class);
        when(timeMetric1.name()).thenReturn("name1");
        TimeMetric timeMetric2 = mock(TimeMetric.class);
        when(timeMetric2.name()).thenReturn("name2");

        expectedException.expect(IllegalArgumentException.class);

        new UnionTimeMetric(ImmutableSet.of(timeMetric1, timeMetric2));
    }

    @Test
    public void elapseTimeInMsShouldUnion() {
        TimeMetric timeMetric1 = mock(TimeMetric.class);
        when(timeMetric1.name()).thenReturn(NAME);
        TimeMetric timeMetric2 = mock(TimeMetric.class);
        when(timeMetric2.name()).thenReturn(NAME);

        new UnionTimeMetric(ImmutableSet.of(timeMetric1, timeMetric2))
            .elapseTimeInMs();

        verify(timeMetric1).elapseTimeInMs();
        verify(timeMetric2).elapseTimeInMs();
    }

    @Test
    public void elapseTimeInMsShouldReturnFirstValue() {
        long firstValue = 18L;
        TimeMetric timeMetric1 = mock(TimeMetric.class);
        when(timeMetric1.name()).thenReturn(NAME);
        when(timeMetric1.elapseTimeInMs()).thenReturn(firstValue);

        TimeMetric timeMetric2 = mock(TimeMetric.class);
        when(timeMetric2.elapseTimeInMs()).thenReturn(19L);
        when(timeMetric2.name()).thenReturn(NAME);

        long actual = new UnionTimeMetric(ImmutableSet.of(timeMetric1, timeMetric2))
            .elapseTimeInMs();

        assertThat(actual).isEqualTo(firstValue);
    }

    @Test
    public void nameShouldReturnUnderlyingValue() {
        TimeMetric timeMetric1 = mock(TimeMetric.class);
        when(timeMetric1.name()).thenReturn(NAME);
        TimeMetric timeMetric2 = mock(TimeMetric.class);
        when(timeMetric2.name()).thenReturn(NAME);

        assertThat(new UnionTimeMetric(ImmutableSet.of(timeMetric1, timeMetric2)).name())
            .isEqualTo(NAME);
    }
}
