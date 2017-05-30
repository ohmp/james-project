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

package org.apache.james.imap.processor.base;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.james.mailbox.MessageUid;

public class UidMsnConverter {

    public final static int FIRST_MSN = 1;

    private final HashBiMap<Integer, MessageUid> msnToUid;

    public UidMsnConverter() {
        msnToUid = HashBiMap.create();
    }

    public synchronized Optional<Integer> getMsn(MessageUid uid) {
        return Optional.fromNullable(msnToUid.inverse().get(uid));
    }

    public synchronized Optional<MessageUid> getUid(int msn) {
        return Optional.fromNullable(msnToUid.get(msn));
    }

    public synchronized Optional<MessageUid> getLastUid() {
        return getUid(getLastMsn());
    }

    public synchronized Optional<MessageUid> getFirstUid() {
        return getUid(FIRST_MSN);
    }

    public synchronized int getNumMessage() {
        return msnToUid.size();
    }

    public synchronized void remove(MessageUid uid) {
        int msn = getMsn(uid).get();
        msnToUid.remove(msn);

        for (int aMsn = msn + 1; aMsn <= getNumMessage() + 1; aMsn++) {
            MessageUid aUid = msnToUid.remove(aMsn);
            addMapping(aMsn - 1, aUid);
        }
    }

    public synchronized boolean isEmpty() {
        return msnToUid.isEmpty();
    }

    public synchronized void clear() {
        msnToUid.clear();
    }

    public synchronized void addUid(MessageUid uid) {
        if (isLastUid(uid)) {
            addMapping(nextMsn(), uid);
        } else {
            addUidInMiddle(uid);
        }
    }

    private boolean isLastUid(MessageUid uid) {
        return msnToUid.isEmpty() ||
            uid.asLong() > msnToUid.get(getLastMsn()).asLong();
    }

    private boolean alreadyContains(MessageUid uid) {
        return msnToUid.containsValue(uid);
    }

    private void addUidInMiddle(MessageUid uid) {
        List<MessageUid> aboveUids = removeAndGetAboveUidSortedInIncreasingOrder(uid);
        addMapping(nextMsn(), uid);
        for (MessageUid aboveUid : aboveUids) {
            addMapping(nextMsn(), aboveUid);
        }
    }

    private List<MessageUid> removeAndGetAboveUidSortedInIncreasingOrder(MessageUid uid) {
        ImmutableList.Builder<MessageUid> result = ImmutableList.builder();
        int position = getLastMsn();
        Optional<MessageUid> maxUid = getUid(position);
        while (maxUid.isPresent() && uid.asLong() < maxUid.get().asLong()) {
            msnToUid.remove(position);
            result.add(maxUid.get());
            position--;
            maxUid = getUid(position);
        }
        return Lists.reverse(result.build());
    }

    @VisibleForTesting
    ImmutableBiMap<Integer, MessageUid> getInternals() {
        return ImmutableBiMap.copyOf(msnToUid);
    }

    private void addMapping(Integer msn, MessageUid uid) {
        if (!msnToUid.containsValue(uid)) {
            msnToUid.forcePut(msn, uid);
        }
    }

    private int nextMsn() {
        return getNumMessage() + FIRST_MSN;
    }

    private int getLastMsn() {
        return getNumMessage();
    }
}
