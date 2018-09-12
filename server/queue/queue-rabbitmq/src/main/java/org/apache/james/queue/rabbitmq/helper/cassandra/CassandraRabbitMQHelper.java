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

import java.util.Iterator;

import org.apache.james.queue.api.ManageableMailQueue;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.helper.api.RabbitMQMailQueueHelper;
import org.apache.mailet.Mail;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

public class CassandraRabbitMQHelper implements RabbitMQMailQueueHelper {

    class CassandraMailQueueIterator implements ManageableMailQueue.MailQueueIterator {

        private final Iterator<ManageableMailQueue.MailQueueItemView> iterator;

        public CassandraMailQueueIterator(Iterator<ManageableMailQueue.MailQueueItemView> iterator) {
            Preconditions.checkNotNull(iterator);

            this.iterator = iterator;
        }

        @Override
        public void close() {}

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public ManageableMailQueue.MailQueueItemView next() {
            return iterator.next();
        }
    }

    private final StoreMailHelper daoHelper;
    private final BrowseHelper browseHelper;
    private final DeleteMailHelper deleteMailHelper;

    private final MailQueueName mailQueueName;

    public CassandraRabbitMQHelper(StoreMailHelper daoHelper,
                                   MailQueueName mailQueueName,
                                   BrowseHelper browseHelper,
                                   DeleteMailHelper deleteMailHelper) {
        this.mailQueueName = mailQueueName;
        this.daoHelper = daoHelper;
        this.browseHelper = browseHelper;
        this.deleteMailHelper = deleteMailHelper;
    }

    @Override
    public void storeMail(Mail mail) {
        daoHelper.storeMailInEnqueueTable(mail, mailQueueName)
            .join();
    }

    @Override
    public void deleteMail(Mail mail) {
        deleteMailHelper.updateDeleteTable(mail, mailQueueName)
            .join();
    }

    @Override
    public ManageableMailQueue.MailQueueIterator browse() {
        return new CassandraMailQueueIterator(
            browseHelper.browse(mailQueueName)
                .join()
                .iterator());
    }

    @Override
    public long getSize() {
        return Iterators.size(browse());
    }

}
