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

package org.apache.james.jmap.api.vacation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.james.util.PatchedValue;
import org.junit.Test;

public class VacationPatchTest {

    public static final ZonedDateTime DATE_2014 = ZonedDateTime.parse("2014-04-03T02:01+07:00[Asia/Vientiane]");
    public static final ZonedDateTime DATE_2015 = ZonedDateTime.parse("2015-04-03T02:01+07:00[Asia/Vientiane]");
    public static final ZonedDateTime DATE_2017 = ZonedDateTime.parse("2017-04-03T02:01+07:00[Asia/Vientiane]");
    public static final Vacation VACATION = Vacation.builder()
        .fromDate(Optional.of(DATE_2014))
        .toDate(Optional.of(DATE_2015))
        .enabled(true)
        .subject(Optional.of("subject"))
        .textBody("anyMessage")
        .htmlBody("html Message")
        .build();

    @Test
    public void fromDateShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().fromDate((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void toDateShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().toDate((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void textBodyShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().textBody((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void htmlBodyShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().htmlBody((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void subjectShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().subject((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void isEnabledShouldThrowNPEOnNullInput() {
        assertThatThrownBy(() -> VacationPatch.builder().isEnabled((PatchedValue) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void isIdentityShouldBeTrueWhenUpdateIsEmpty() {
        assertThat(
            VacationPatch.builder()
                .build()
                .isIdentity())
            .isTrue();
    }

    @Test
    public void isIdentityShouldBeFalseWhenUpdateIsNotEmpty() {
        assertThat(
            VacationPatch.builder()
                .subject(PatchedValue.modifyTo("any subject"))
                .build()
                .isIdentity())
            .isFalse();
    }

    @Test
    public void builderShouldWellSetFields() {
        PatchedValue<String> subject = PatchedValue.modifyTo("subject");
        PatchedValue<String> htmlBody = PatchedValue.modifyTo("html text");
        PatchedValue<String> textBody = PatchedValue.modifyTo("simple text");
        PatchedValue<Boolean> isEnabled = PatchedValue.modifyTo(true);

        VacationPatch update = VacationPatch.builder()
            .fromDate(PatchedValue.modifyTo(DATE_2014))
            .toDate(PatchedValue.modifyTo(DATE_2015))
            .subject(subject)
            .htmlBody(htmlBody)
            .textBody(textBody)
            .isEnabled(isEnabled)
            .build();

        assertThat(update.getFromDate()).isEqualTo(PatchedValue.modifyTo(DATE_2014));
        assertThat(update.getToDate()).isEqualTo(PatchedValue.modifyTo(DATE_2015));
        assertThat(update.getSubject()).isEqualTo(subject);
        assertThat(update.getHtmlBody()).isEqualTo(htmlBody);
        assertThat(update.getTextBody()).isEqualTo(textBody);
        assertThat(update.getIsEnabled()).isEqualTo(isEnabled);
    }

    @Test
    public void patchVacationShouldUpdateEnabled() {
        VacationPatch vacationPatch = VacationPatch.builder()
            .isEnabled(PatchedValue.modifyTo(true))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .enabled(true)
                .build());
    }

    @Test
    public void patchVacationShouldUpdateFromDate() {
        VacationPatch vacationPatch = VacationPatch.builder()
            .fromDate(PatchedValue.modifyTo(DATE_2014))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .fromDate(Optional.of(DATE_2014))
                .enabled(false)
                .build());
    }

    @Test
    public void patchVacationShouldUpdateToDate() {
        VacationPatch vacationPatch = VacationPatch.builder()
            .toDate(PatchedValue.modifyTo(DATE_2017))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .toDate(Optional.of(DATE_2017))
                .enabled(false)
                .build());
    }

    @Test
    public void patchVacationShouldUpdateSubject() {
        String newSubject = "new subject";
        VacationPatch vacationPatch = VacationPatch.builder()
            .subject(PatchedValue.modifyTo(newSubject))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .subject(Optional.of(newSubject))
                .enabled(false)
                .build());
    }

    @Test
    public void patchVacationShouldUpdateTextBody() {
        String newTextBody = "new text body";
        VacationPatch vacationPatch = VacationPatch.builder()
            .textBody(PatchedValue.modifyTo(newTextBody))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .textBody(newTextBody)
                .enabled(false)
                .build());
    }

    @Test
    public void patchVacationShouldUpdateHtmlBody() {
        String newHtmlBody = "new <b>html</b> body";
        VacationPatch vacationPatch = VacationPatch.builder()
            .htmlBody(PatchedValue.modifyTo(newHtmlBody))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(Vacation.builder()
                .enabled(false)
                .htmlBody(newHtmlBody)
                .build());
    }

    @Test
    public void patchVacationShouldAllowToUpdateAllFieldsAtOnce() {
        VacationPatch vacationPatch = VacationPatch.builder()
            .subject(PatchedValue.ofOptional(VACATION.getSubject()))
            .textBody(PatchedValue.ofOptional(VACATION.getTextBody()))
            .htmlBody(PatchedValue.ofOptional(VACATION.getHtmlBody()))
            .fromDate(PatchedValue.ofOptional(VACATION.getFromDate()))
            .toDate(PatchedValue.ofOptional(VACATION.getToDate()))
            .isEnabled(PatchedValue.modifyTo(VACATION.isEnabled()))
            .build();

        assertThat(vacationPatch.patch(VacationRepository.DEFAULT_VACATION))
            .isEqualTo(VACATION);
    }

    @Test
    public void emptyPatchesShouldNotChangeExistingVacations() {
        assertThat(VacationPatch.builder()
            .build()
            .patch(VACATION))
            .isEqualTo(VACATION);
    }

    @Test
    public void nullUpdateShouldResetSubject() {
        Vacation vacation = VacationPatch.builderFrom(VACATION)
            .subject(PatchedValue.remove())
            .build()
            .patch(VACATION);

        assertThat(vacation.getSubject()).isEmpty();
        assertThat(vacation)
            .isEqualTo(Vacation.builder()
                .fromDate(VACATION.getFromDate())
                .toDate(VACATION.getToDate())
                .enabled(VACATION.isEnabled())
                .textBody(VACATION.getTextBody())
                .htmlBody(VACATION.getHtmlBody())
                .build());
    }

    @Test
    public void nullUpdateShouldResetText() {
        Vacation vacation = VacationPatch.builderFrom(VACATION)
            .textBody(PatchedValue.remove())
            .build()
            .patch(VACATION);

        assertThat(vacation.getTextBody()).isEmpty();
        assertThat(vacation)
            .isEqualTo(Vacation.builder()
                .fromDate(VACATION.getFromDate())
                .toDate(VACATION.getToDate())
                .enabled(VACATION.isEnabled())
                .subject(VACATION.getSubject())
                .htmlBody(VACATION.getHtmlBody())
                .build());
    }

    @Test
    public void nullUpdateShouldResetHtml() {
        Vacation vacation = VacationPatch.builderFrom(VACATION)
            .htmlBody(PatchedValue.remove())
            .build()
            .patch(VACATION);

        assertThat(vacation.getHtmlBody()).isEmpty();
        assertThat(vacation)
            .isEqualTo(Vacation.builder()
                .fromDate(VACATION.getFromDate())
                .toDate(VACATION.getToDate())
                .enabled(VACATION.isEnabled())
                .subject(VACATION.getSubject())
                .textBody(VACATION.getTextBody())
                .build());
    }

    @Test
    public void nullUpdateShouldResetToDate() {
        Vacation vacation = VacationPatch.builderFrom(VACATION)
            .toDate(PatchedValue.remove())
            .build()
            .patch(VACATION);

        assertThat(vacation.getToDate()).isEmpty();
        assertThat(vacation)
            .isEqualTo(Vacation.builder()
                .fromDate(VACATION.getFromDate())
                .enabled(VACATION.isEnabled())
                .subject(VACATION.getSubject())
                .textBody(VACATION.getTextBody())
                .htmlBody(VACATION.getHtmlBody())
                .build());
    }

    @Test
    public void nullUpdateShouldResetFromDate() {
        Vacation vacation = VacationPatch.builderFrom(VACATION)
            .fromDate(PatchedValue.remove())
            .build()
            .patch(VACATION);

        assertThat(vacation.getFromDate()).isEmpty();
        assertThat(vacation)
            .isEqualTo(Vacation.builder()
                .toDate(VACATION.getToDate())
                .enabled(VACATION.isEnabled())
                .subject(VACATION.getSubject())
                .textBody(VACATION.getTextBody())
                .htmlBody(VACATION.getHtmlBody())
                .build());
    }

}
