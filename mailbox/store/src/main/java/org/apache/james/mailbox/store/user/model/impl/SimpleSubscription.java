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
package org.apache.james.mailbox.store.user.model.impl;

import org.apache.james.mailbox.store.user.model.Subscription;

public class SimpleSubscription implements Subscription {

    private final String user;
    private final String mailbox;
    
    public SimpleSubscription(String user, String mailbox) {
        this.user = user;
        this.mailbox = mailbox;
    }

    public String getMailbox() {
        return mailbox;
    }

    public String getUser() {
        return user;
    }

    /*
    Question 0 :

    Extends Equals and hashCode
    You will check both user and mailbox
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleSubscription that = (SimpleSubscription) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        return mailbox != null ? mailbox.equals(that.mailbox) : that.mailbox == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (mailbox != null ? mailbox.hashCode() : 0);
        return result;
    }
}
