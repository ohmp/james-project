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

package org.apache.james.blob.objectstorage;

import java.util.Arrays;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class BufferPool {
    static final int BUFFERED_SIZE = 1024 * 1024;
    private static final int POOL_SIZE = 128;

    private static class BufferFactory extends BasePooledObjectFactory<byte[]> {
        @Override
        public byte[] create() {
            return new byte[BUFFERED_SIZE + 1];
        }

        @Override
        public PooledObject<byte[]> wrap(byte[] o) {
            return new DefaultPooledObject<>(o);
        }
    }

    private final GenericObjectPool<byte[]> objectPool;

    public BufferPool() {
        GenericObjectPoolConfig<byte[]> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(POOL_SIZE);
        objectPool = new GenericObjectPool<>(new BufferFactory());
    }

    public byte[] borrowBuffer() {
        try {
            return objectPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void returnBuffer(byte[] buffer) {
        Arrays.fill(buffer, (byte)0);
        objectPool.returnObject(buffer);
    }
}
