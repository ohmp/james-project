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

package org.apache.james.metrics.dropwizard;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.james.metrics.api.Gauge;
import org.apache.james.metrics.api.GaugeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.MetricRegistry;

public class DropWizardGaugeRegistry implements GaugeRegistry {

    static class ErrorHandlingGauge<T> implements com.codahale.metrics.Gauge<T> {
        private final String name;
        private final com.codahale.metrics.Gauge<T> underlying;

        ErrorHandlingGauge(String name, com.codahale.metrics.Gauge<T> underlying) {
            this.name = name;
            this.underlying = underlying;
        }

        @Override
        public T getValue() {
            try {
                return underlying.getValue();
            } catch (Exception e) {
                LOGGER.error("Error reading gauge {}", name, e);
                return null;
            }
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DropWizardGaugeRegistry.class);

    private static final int CACHE_EXPIRACY_IN_SECONDS = 10;

    private final MetricRegistry metricRegistry;

    @Inject
    public DropWizardGaugeRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public <T> GaugeRegistry register(String name, Gauge<T> gauge) {
        metricRegistry.gauge(name, () ->
            handleError(name,
                toCachedGauge(gauge)));
        return this;
    }

    private <T> CachedGauge<T> toCachedGauge(Gauge<T> gauge) {
        return new CachedGauge<T>(CACHE_EXPIRACY_IN_SECONDS, TimeUnit.SECONDS) {
            @Override
            protected T loadValue() {
                return gauge.get();
            }
        };
    }

    private <T> ErrorHandlingGauge<T> handleError(String name, com.codahale.metrics.Gauge<T> gauge) {
        return new ErrorHandlingGauge<>(name, gauge);
    }
}
