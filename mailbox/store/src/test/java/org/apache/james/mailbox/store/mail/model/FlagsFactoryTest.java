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
package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class FlagsFactoryTest {

    private ImmutableList<Flag> listOfSystemFlags = ImmutableList.of(Flag.SEEN, Flag.RECENT, Flag.ANSWERED);
    private ImmutableList<String> listOfUserFlags = ImmutableList.of("userFlag", "soCool");
    private Flags emptyFlags;
    private Flags someFlags;

    @Before
    public void setup() {
        emptyFlags = new Flags();
        someFlags = new Flags();
        listOfSystemFlags.forEach(someFlags::add);
        listOfUserFlags.forEach(someFlags::add);
    }

    @Test
    public void builderShouldThrowWhenNoFlagsProvided() {
        assertThatThrownBy(() -> FlagsFactory.builder().build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void builderShouldNotRequireFlagsInstanceWhenUserFlagsDefined() {
        Flags actual = FlagsFactory.builder().addUserFlags("userFlag").build();
        assertThat(actual.getUserFlags()).containsOnly("userFlag");
    }


    @Test
    public void builderShouldNotRequireUserFlagsWhenFlagsInstanceDefined() {
        assertThat(FlagsFactory.builder().flags(new Flags()).build()).isNotNull();
    }

    @Test
    public void builderShouldFilterUserFlags() {
        Flags actual = FlagsFactory.builder()
            .flags(someFlags)
            .filteringFlags(
                FlagsFilter
                    .builder()
                    .userFlagFilter(f -> f.equals("soCool"))
                    .build())
            .build();
        assertThat(actual.getUserFlags()).containsOnly("soCool");
    }

    @Test
    public void builderShouldFilterSystemFlags() {
        Flags actual = FlagsFactory.builder()
            .flags(someFlags)
            .filteringFlags(
                FlagsFilter
                    .builder()
                    .systemFlagFilter(f -> f.equals(Flag.SEEN))
                    .build())
            .build();
        assertThat(actual.getSystemFlags()).containsOnly(Flag.SEEN);
    }

    @Test
    public void builderShouldAllowFilteringOnEmptyFlags() {
        Flags actual = FlagsFactory.builder()
            .flags(emptyFlags)
            .filteringFlags(
                FlagsFilter
                    .builder()
                    .systemFlagFilter(f -> f.equals(Flag.SEEN))
                    .userFlagFilter(f -> f.equals("soCool"))
                    .build())
            .build();
        assertThat(actual).isNotNull();
    }


}