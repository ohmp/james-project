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

package org.apache.james.dlp.api;

import java.util.Objects;

import com.google.common.base.MoreObjects;

public class DLPRule {

    public static class Targets {
        private final boolean senderTargeted;
        private final boolean recipientTargeted;
        private final boolean contentTargeted;

        public Targets(boolean senderTargeted, boolean recipientTargeted, boolean contentTargeted) {
            this.senderTargeted = senderTargeted;
            this.recipientTargeted = recipientTargeted;
            this.contentTargeted = contentTargeted;
        }

        public boolean isSenderTargeted() {
            return senderTargeted;
        }

        public boolean isRecipientTargeted() {
            return recipientTargeted;
        }

        public boolean isContentTargeted() {
            return contentTargeted;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof Targets) {
                Targets targets = (Targets) o;

                return Objects.equals(this.senderTargeted, targets.senderTargeted)
                    && Objects.equals(this.recipientTargeted, targets.recipientTargeted)
                    && Objects.equals(this.contentTargeted, targets.contentTargeted);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(senderTargeted, recipientTargeted, contentTargeted);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("senderTargeted", senderTargeted)
                .add("recipientTargeted", recipientTargeted)
                .add("contentTargeted", contentTargeted)
                .toString();
        }
    }

    private final String explanation;
    private final String regexp;
    private final Targets targets;

    public DLPRule(String explanation, String regexp, Targets targets) {
        this.explanation = explanation;
        this.regexp = regexp;
        this.targets = targets;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getRegexp() {
        return regexp;
    }

    public Targets getTargets() {
        return targets;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof DLPRule) {
            DLPRule dlpRule = (DLPRule) o;

            return Objects.equals(this.explanation, dlpRule.explanation)
                && Objects.equals(this.regexp, dlpRule.regexp)
                && Objects.equals(this.targets, dlpRule.targets);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(explanation, regexp, targets);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("explanation", explanation)
            .add("regexp", regexp)
            .add("targets", targets)
            .toString();
    }
}
