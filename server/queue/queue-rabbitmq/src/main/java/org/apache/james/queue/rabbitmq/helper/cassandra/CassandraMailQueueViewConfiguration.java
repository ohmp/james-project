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

package org.apache.james.queue.rabbitmq.helper.cassandra;

import java.time.Duration;

import com.google.common.base.Preconditions;

public class CassandraMailQueueViewConfiguration {
    interface Builder {
        @FunctionalInterface
        interface RequireBucketCount {
            RequireUpdateBrowseStartPace bucketCount(int bucketCount);
        }

        @FunctionalInterface
        interface RequireUpdateBrowseStartPace {
            RequireSliceWindow updateBrowseStartPace(int updateBrowseStartPace);
        }

        @FunctionalInterface
        interface RequireSliceWindow {
            LastStage sliceWindow(Duration sliceWindow);
        }

        class LastStage {
            private final int bucketCount;
            private final int updateBrowseStartPace;
            private final Duration sliceWindow;

            private LastStage(int bucketCount, int updateBrowseStartPace, Duration sliceWindow) {
                this.bucketCount = bucketCount;
                this.updateBrowseStartPace = updateBrowseStartPace;
                this.sliceWindow = sliceWindow;
            }

            public CassandraMailQueueViewConfiguration build() {
                Preconditions.checkNotNull(sliceWindow, "'sliceWindow' is compulsory");
                Preconditions.checkState(bucketCount > 0, "'bucketCount' needs to be a strictly positive integer");
                Preconditions.checkState(updateBrowseStartPace > 0, "'updateBrowseStartPace' needs to be a strictly positive integer");

                return new CassandraMailQueueViewConfiguration(bucketCount, updateBrowseStartPace, sliceWindow);
            }
        }
    }

    public static Builder.RequireBucketCount builder() {
        return bucketCount -> updateBrowseStartPace -> sliceWindow -> new Builder.LastStage(bucketCount, updateBrowseStartPace, sliceWindow);
    }

    private final int bucketCount;
    private final int updateBrowseStartPace;
    private final Duration sliceWindow;

    private CassandraMailQueueViewConfiguration(int bucketCount,
                                                int updateBrowseStartPace,
                                                Duration sliceWindow) {
        this.bucketCount = bucketCount;
        this.updateBrowseStartPace = updateBrowseStartPace;
        this.sliceWindow = sliceWindow;
    }

    public int getUpdateBrowseStartPace() {
        return updateBrowseStartPace;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public Duration getSliceWindow() {
        return sliceWindow;
    }

    public long getSliceWindowInSecond() {
        return sliceWindow.getSeconds();
    }
}
