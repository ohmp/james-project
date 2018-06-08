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

package org.apache.james.mailrepository.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public interface MailRepositoryUrlStoreContract {
    MailRepositoryUrl URL_1 = new MailRepositoryUrl("proto://var/mail/toto");
    MailRepositoryUrl URL_2 = new MailRepositoryUrl("proto://var/mail/tata");

    @Test
    default void shouldBeEmptyByDefault(MailRepositoryUrlStore store) {
        assertThat(store.retrieveUsedUrls()).isEmpty();
    }

    @Test
    default void retrieveusedUrlShouldReturnAddedUrl(MailRepositoryUrlStore store) {
        store.addUrl(URL_1);

        assertThat(store.retrieveUsedUrls()).containsOnly(URL_1);
    }

    @Test
    default void retrieveusedUrlShouldNotReturnDuplicates(MailRepositoryUrlStore store) {
        store.addUrl(URL_1);
        store.addUrl(URL_1);

        assertThat(store.retrieveUsedUrls()).containsOnly(URL_1);
    }

    @Test
    default void retrieveusedUrlShouldReturnAddedUrls(MailRepositoryUrlStore store) {
        store.addUrl(URL_1);
        store.addUrl(URL_2);

        assertThat(store.retrieveUsedUrls()).containsOnly(URL_1, URL_2);
    }

    @Test
    default void containsShouldReturnFalseWhenNotExisting(MailRepositoryUrlStore store) {
        assertThat(store.contains(URL_1)).isFalse();
    }

    @Test
    default void containsShouldReturnTrueWhenExisting(MailRepositoryUrlStore store) {
        store.addUrl(URL_1);

        assertThat(store.contains(URL_1)).isTrue();
    }

}