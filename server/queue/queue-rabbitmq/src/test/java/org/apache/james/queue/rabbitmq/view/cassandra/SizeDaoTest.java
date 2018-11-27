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

package org.apache.james.queue.rabbitmq.view.cassandra;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SizeDaoTest {
    private static final MailQueueName OUT_GOING = MailQueueName.fromString("OUT_GOING_1");

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraMailQueueViewModule.MODULE);

    private SizeDao testee;

    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        testee = new SizeDao(cassandra.getConf());
    }

    @Test
    void getSizeShouldReturnEmptyByDefault() {
        assertThat(testee.getSize(OUT_GOING).join()).isEmpty();
    }

    @Test
    void getSizeShouldBeIncrementedAfterIncrementCall() {
        testee.increment(OUT_GOING).join();

        assertThat(testee.getSize(OUT_GOING).join()).contains(1L);
    }

    @Test
    void getSizeShouldBeIncrementedMultipleTimesAfterSeveralIncrementCall() {
        testee.increment(OUT_GOING).join();
        testee.increment(OUT_GOING).join();

        assertThat(testee.getSize(OUT_GOING).join()).contains(2L);
    }

    @Test
    void getSizeShouldBeDecrementedAfterDecrementCall() {
        testee.increment(OUT_GOING).join();
        testee.increment(OUT_GOING).join();

        testee.decrement(OUT_GOING).join();

        assertThat(testee.getSize(OUT_GOING).join()).contains(1L);
    }
}