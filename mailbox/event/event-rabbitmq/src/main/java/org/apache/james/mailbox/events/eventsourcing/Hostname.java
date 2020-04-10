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

package org.apache.james.mailbox.events.eventsourcing;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

public class Hostname {
    public static Hostname of(String value) {
        Preconditions.checkNotNull(value, "'value' should not be null");
        Preconditions.checkArgument(StringUtils.isBlank(value), "'value' ");

        return new Hostname(value);
    }

    public static Hostname localHost() {
        try {
            return new Hostname(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final String value;

    private Hostname(String value) {
        this.value = value;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Hostname) {
            Hostname hostname = (Hostname) o;

            return Objects.equals(this.value, hostname.value);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }
}
