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

package org.apache.james.jmap.methods;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import javax.mail.MessagingException;

import org.apache.james.jmap.model.SetMessagesResponse;
import org.apache.james.mailbox.exception.MailboxException;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class SetMessagePipeline {
    @FunctionalInterface
    public interface Step {
        Optional<SetMessagesResponse.Builder> run(SetMessagesResponse.Builder responseBuilder) throws MailboxException, MessagingException;
    }

    @FunctionalInterface
    public interface Operation {
        SetMessagesResponse.Builder run(SetMessagesResponse.Builder responseBuilder) throws MailboxException, MessagingException;
    }

    @FunctionalInterface
    public interface MailboxConditionSupplier {
        boolean get() throws MailboxException;
    }

    public static class ConditionalStep implements Step {
        public static class Factory {
            private MailboxConditionSupplier condition;

            public Factory when(MailboxConditionSupplier condition) {
                this.condition = condition;
                return this;
            }

            public ConditionalStep then(Operation operation) {
                Preconditions.checkNotNull(condition);
                Preconditions.checkNotNull(operation);
                return new ConditionalStep(condition, operation);
            }

            public ConditionalStep then(SetMessagePipeline pipeline) {
                Preconditions.checkNotNull(condition);
                Preconditions.checkNotNull(pipeline);
                return new ConditionalStep(condition, nested(pipeline));
            }
        }
        private final MailboxConditionSupplier condition;
        private final Operation operation;

        public ConditionalStep(MailboxConditionSupplier condition, Operation operation) {
            this.condition = condition;
            this.operation = operation;
        }

        @Override
        public Optional<SetMessagesResponse.Builder> run(SetMessagesResponse.Builder responseBuilder) throws MailboxException, MessagingException {
            if (condition.get()) {
                return Optional.of(operation.run(responseBuilder));
            }
            return Optional.empty();
        }
    }

    public static ConditionalStep.Factory when(MailboxConditionSupplier condition) {
        return new ConditionalStep.Factory()
            .when(condition);
    }

    public static ConditionalStep.Factory when(boolean condition) {
        return new ConditionalStep.Factory()
            .when(() -> condition);
    }

    public static Operation nested(SetMessagePipeline pipeline) {
        return pipeline::executeFirst;
    }

    public static SetMessagePipeline forOperations(Step... steps) {
        return new SetMessagePipeline(ImmutableList.copyOf(Arrays.asList(steps)));
    }

    public static Step endWith(Operation operation) {
        return builder -> Optional.of(operation.run(builder));
    }

    private final ImmutableList<Step> steps;

    private SetMessagePipeline(ImmutableList<Step> steps) {
        this.steps = steps;
    }

    public SetMessagesResponse.Builder executeFirst(SetMessagesResponse.Builder responseBuilder) throws MailboxException, MessagingException {
        FunctionChainer<Step, Optional<SetMessagesResponse.Builder>> runOperation =
            Throwing.function(step -> step.run(responseBuilder));

        return steps.stream()
            .map(runOperation.sneakyThrow())
            .filter(Optional::isPresent)
            .findFirst()
            .flatMap(Function.identity())
            .orElse(responseBuilder);
    }
}
