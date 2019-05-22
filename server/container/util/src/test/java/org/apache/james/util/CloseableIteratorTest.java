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

package org.apache.james.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

class CloseableIteratorTest {
    private static final Closeable THROW_RUNTIME = () -> {
        throw new RuntimeException();
    };
    private static final Closeable THROW_IO_EXCEPTION = () -> {
        throw new IOException();
    };

    @Test
    void closeableIteratorShouldPreserveWrappedIteratorContent() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> { });

        assertThat(testee).containsExactly(1, 2, 3);
    }

    @Test
    void propagateExceptionCloseableIteratorShouldPreserveWrappedIteratorContent() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> { });

        assertThat(testee.propagateException()).containsExactly(1, 2, 3);
    }

    @Test
    void closeableIteratorShouldPreserveWrappedIteratorContentWhenEmpty() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.<Integer>of().iterator(), () -> { });

        assertThat(testee).isEmpty();
    }

    @Test
    void propagateExceptionCloseableIteratorShouldPreserveWrappedIteratorContentWhenEmpty() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.<Integer>of().iterator(), () -> { });

        assertThat(testee.propagateException()).isEmpty();
    }

    @Test
    void closeShouldCallWrappedCallable() throws Exception {
        AtomicBoolean open = new AtomicBoolean(true);
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> open.set(false));

        testee.close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void closePropagateExceptionShouldCallWrappedCallable() {
        AtomicBoolean open = new AtomicBoolean(true);
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> open.set(false));

        testee.propagateException().close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void fromStreamShouldPreserveStreamContent() {
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> { });
        CloseableIterator<Integer> testee = CloseableIterator.Impl.fromStream(stream);

        assertThat(testee).containsExactly(1, 2, 3);
    }

    @Test
    void closeShouldCallWrappedStreamClose() throws Exception {
        AtomicBoolean open = new AtomicBoolean(true);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> open.set(false));
        CloseableIterator<Integer> testee = CloseableIterator.Impl.fromStream(stream);

        testee.close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void closePropagateExceptionShouldCallWrappedStreamClose() {
        AtomicBoolean open = new AtomicBoolean(true);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> open.set(false));
        CloseableIterator<Integer> testee = CloseableIterator.Impl.fromStream(stream);

        testee.propagateException().close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void streamShouldSetupOnCloseCallback() {
        AtomicBoolean open = new AtomicBoolean(true);
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> open.set(false));

        testee.stream().close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void streamShouldSetupOnCloseCallbackWhenPropagateException() {
        AtomicBoolean open = new AtomicBoolean(true);
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1, 2, 3).iterator(), () -> open.set(false));

        testee.propagateException().stream().close();

        assertThat(open.get()).isFalse();
    }

    @Test
    void closeShouldPropagateRuntime() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_RUNTIME);

        assertThatThrownBy(testee::close).isInstanceOf(RuntimeException.class);
    }

    @Test
    void closePropagateExceptionShouldPropagateRuntime() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_RUNTIME);

        assertThatThrownBy(() -> testee.propagateException().close()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void closeShouldPropagateIOException() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_IO_EXCEPTION);

        assertThatThrownBy(testee::close).isInstanceOf(IOException.class);
    }

    @Test
    void closePropagateExceptionShouldWrapIOExceptionIntoRuntime() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_IO_EXCEPTION);

        assertThatThrownBy(() -> testee.propagateException().close()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void closeStreamShouldPropagateRuntime() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_RUNTIME);

        assertThatThrownBy(() -> testee.stream().close()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void closeStreamShouldWrapIOExceptionIntoRuntime() {
        CloseableIterator<Integer> testee = new CloseableIterator.Impl<>(ImmutableList.of(1).iterator(), THROW_IO_EXCEPTION);

        assertThatThrownBy(() -> testee.stream().close()).isInstanceOf(RuntimeException.class);
    }
}