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

import org.apache.james.metrics.api.TimeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

public class DropWizardTimeMetric implements TimeMetric {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropWizardTimeMetric.class);

    static class DropWizardExecutionResult implements ExecutionResult {
        private final String name;
        private final long elaspedInNanoSeconds;
        private final long p99InNanoSeconds;

        DropWizardExecutionResult(String name, long elaspedInNanoSeconds, long p99InNanoSeconds) {
            Preconditions.checkArgument(elaspedInNanoSeconds > 0);
            Preconditions.checkArgument(p99InNanoSeconds > 0);

            this.name = name;
            this.elaspedInNanoSeconds = elaspedInNanoSeconds;
            this.p99InNanoSeconds = p99InNanoSeconds;
        }

        @Override
        public long elaspedInNanoSeconds() {
            return elaspedInNanoSeconds;
        }

        @Override
        public ExecutionResult logWhenExceedP99(long thresholdInNanoSeconds) {
            Preconditions.checkArgument(thresholdInNanoSeconds > 0);
            if (elaspedInNanoSeconds > p99InNanoSeconds && elaspedInNanoSeconds > thresholdInNanoSeconds) {
                LOGGER.info("{} metrics took {} nano seconds to complete, exceeding its {} nano seconds p99",
                    name, elaspedInNanoSeconds, p99InNanoSeconds);
            }
            return this;
        }
    }

    private final String name;
    private final Timer.Context context;
    private final Timer timer;

    public DropWizardTimeMetric(String name, Timer timer) {
        this.name = name;
        this.timer = timer;
        this.context = this.timer.time();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ExecutionResult stopAndPublish() {
        return new DropWizardExecutionResult(name, context.stop(), Math.round(timer.getSnapshot().get999thPercentile()));
    }
}
