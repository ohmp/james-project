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

package org.apache.james.mailbox.store.mail.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.mailbox.model.MailboxAnnotation;
import org.apache.james.mailbox.model.MailboxAnnotationKey;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

public interface AnnotationMapperContract {
    MailboxAnnotationKey PRIVATE_USER_KEY = new MailboxAnnotationKey("/private/commentuser");
    MailboxAnnotationKey PRIVATE_UPPER_CASE_KEY = new MailboxAnnotationKey("/PRIVATE/COMMENT");
    MailboxAnnotationKey PRIVATE_KEY = new MailboxAnnotationKey("/private/comment");
    MailboxAnnotationKey PRIVATE_CHILD_KEY = new MailboxAnnotationKey("/private/comment/user");
    MailboxAnnotationKey PRIVATE_GRANDCHILD_KEY = new MailboxAnnotationKey("/private/comment/user/name");
    MailboxAnnotationKey SHARED_KEY = new MailboxAnnotationKey("/shared/comment");

    MailboxAnnotation PRIVATE_USER_ANNOTATION = MailboxAnnotation.newInstance(PRIVATE_USER_KEY, "My private comment");
    MailboxAnnotation PRIVATE_ANNOTATION = MailboxAnnotation.newInstance(PRIVATE_KEY, "My private comment");
    MailboxAnnotation PRIVATE_ANNOTATION_WITH_KEY_UPPER = MailboxAnnotation.newInstance(PRIVATE_UPPER_CASE_KEY, "The annotation with upper key");
    MailboxAnnotation PRIVATE_CHILD_ANNOTATION = MailboxAnnotation.newInstance(PRIVATE_CHILD_KEY, "My private comment");
    MailboxAnnotation PRIVATE_ANNOTATION_UPDATE = MailboxAnnotation.newInstance(PRIVATE_KEY, "My updated private comment");
    MailboxAnnotation SHARED_ANNOTATION =  MailboxAnnotation.newInstance(SHARED_KEY, "My shared comment");

    MailboxAnnotation PRIVATE_GRANDCHILD_ANNOTATION = MailboxAnnotation.newInstance(PRIVATE_GRANDCHILD_KEY, "My private comment");

    MailboxId mailboxId();

    AnnotationMapper annotationMapper();

    @Test
    default void insertAnnotationShouldThrowExceptionWithNilData() {
        assertThatThrownBy(() -> annotationMapper().insertAnnotation(mailboxId(), MailboxAnnotation.nil(PRIVATE_KEY)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void insertAnnotationShouldCreateNewAnnotation() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);

        assertThat(annotationMapper().getAllAnnotations(mailboxId())).containsExactly(PRIVATE_ANNOTATION);
    }

    @Test
    default void insertAnnotationShouldUpdateExistedAnnotation() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION_UPDATE);

        assertThat(annotationMapper().getAllAnnotations(mailboxId())).containsExactly(PRIVATE_ANNOTATION_UPDATE);
    }

    @Test
    default void deleteAnnotationShouldDeleteStoredAnnotation() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().deleteAnnotation(mailboxId(), PRIVATE_KEY);

        assertThat(annotationMapper().getAllAnnotations(mailboxId())).isEmpty();
    }

    @Test
    default void getEmptyAnnotationsWithNonStoredAnnotations() {
        assertThat(annotationMapper().getAllAnnotations(mailboxId())).isEmpty();
    }

    @Test
    default void getAllAnnotationsShouldRetrieveStoredAnnotations() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);

        assertThat(annotationMapper().getAllAnnotations(mailboxId())).containsOnly(PRIVATE_ANNOTATION, SHARED_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysShouldReturnStoredAnnotationWithFilter() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeys(mailboxId(), ImmutableSet.of(PRIVATE_KEY)))
            .containsOnly(PRIVATE_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysWithOneDepthShouldReturnThatEntryAndItsChildren() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_GRANDCHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithOneDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY)))
            .containsOnly(PRIVATE_ANNOTATION, PRIVATE_CHILD_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysWithAllDepthShouldReturnThatEntryAndAllBelowEntries() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_GRANDCHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithAllDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY)))
            .containsOnly(PRIVATE_ANNOTATION, PRIVATE_CHILD_ANNOTATION, PRIVATE_GRANDCHILD_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysWithOneDepthShouldReturnTheChildrenEntriesEvenItDoesNotExist() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_GRANDCHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithOneDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY)))
            .containsOnly(PRIVATE_CHILD_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysWithAllDepthShouldReturnTheChildrenEntriesEvenItDoesNotExist() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_GRANDCHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithAllDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY)))
            .containsOnly(PRIVATE_CHILD_ANNOTATION, PRIVATE_GRANDCHILD_ANNOTATION);
    }

    @Test
    default void getAnnotationsByKeysWithOneDepthShouldReturnEmptyWithEmptyInputKeys() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_CHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_GRANDCHILD_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithOneDepth(mailboxId(), ImmutableSet.<MailboxAnnotationKey>of())).isEmpty();
    }

    @Test
    default void getAnnotationsByKeysWithOneDepthShouldReturnEmptyIfDoNotFind() {
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithOneDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY))).isEmpty();
    }

    @Test
    default void getAnnotationsByKeysWithAllDepthShouldReturnEmptyIfDoNotFind() {
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().getAnnotationsByKeysWithAllDepth(mailboxId(), ImmutableSet.of(PRIVATE_KEY))).isEmpty();
    }

    @Test
    default void annotationShouldBeCaseInsentive() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION_WITH_KEY_UPPER);

        assertThat(annotationMapper().getAllAnnotations(mailboxId())).containsOnly(PRIVATE_ANNOTATION_WITH_KEY_UPPER);
    }

    @Test
    default void isExistedShouldReturnTrueIfAnnotationIsStored() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);

        assertThat(annotationMapper().exist(mailboxId(), PRIVATE_ANNOTATION)).isTrue();
    }

    @Test
    default void isExistedShouldReturnFalseIfAnnotationIsNotStored() {
        assertThat(annotationMapper().exist(mailboxId(), PRIVATE_ANNOTATION)).isFalse();
    }

    @Test
    default void countAnnotationShouldReturnZeroIfNoMoreAnnotationBelongToMailbox() {
        assertThat(annotationMapper().countAnnotations(mailboxId())).isEqualTo(0);
    }

    @Test
    default void countAnnotationShouldReturnNumberOfAnnotationBelongToMailbox() {
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_ANNOTATION_UPDATE);
        annotationMapper().insertAnnotation(mailboxId(), SHARED_ANNOTATION);
        annotationMapper().insertAnnotation(mailboxId(), PRIVATE_USER_ANNOTATION);

        assertThat(annotationMapper().countAnnotations(mailboxId())).isEqualTo(3);
    }
}
