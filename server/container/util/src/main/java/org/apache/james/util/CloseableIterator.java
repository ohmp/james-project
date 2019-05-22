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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.james.util.streams.Iterators;

public interface CloseableIterator<T> extends Iterator<T>, Closeable {
    interface PropagateException<T> extends Iterator<T>, Closeable {
        void close();

        default Stream<T> stream() {
            return Iterators.toStream(this)
                .onClose(this::close);
        }

        class Impl<T> implements PropagateException<T> {
            private final CloseableIterator<T> iterator;

            public Impl(CloseableIterator<T> iterator) {
                this.iterator = iterator;
            }

            @Override
            public void close() {
                try {
                    iterator.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next();
            }
        }
    }

    class Impl<T> implements CloseableIterator<T> {
        public static <T> CloseableIterator<T> fromStream(Stream<T> stream) {
            return new Impl<>(stream.iterator(), stream::close);
        }

        public static <T> CloseableIterator<T> from(Iterator<T> iterator) {
            return new Impl<>(iterator, () -> { });
        }

        private final Iterator<T> iterator;
        private final Closeable closeable;

        public Impl(Iterator<T> iterator, Closeable closeable) {
            this.iterator = iterator;
            this.closeable = closeable;
        }

        @Override
        public void close() throws IOException {
            closeable.close();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }

    default Stream<T> stream() {
        return propagateException().stream();
    }

    default PropagateException<T> propagateException() {
        return new PropagateException.Impl<>(this);
    }
}
