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

package org.apache.mailet.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.mail.MessagingException;

import org.apache.james.util.Port;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Test;

public class MailetUtilTest {

    private static final String A_PARAMETER = "aParameter";
    public static final int DEFAULT_VALUE = 36;

    @Test
    public void getInitParameterShouldReturnTrueWhenIsValueTrueLowerCase() {
        assertThat(getParameterValued("true", false)).isTrue();
    }

    @Test
    public void getInitParameterShouldReturnTrueWhenIsValueTrueUpperCase() {
        assertThat(getParameterValued("TRUE", false)).isTrue();
    }

    @Test
    public void getInitParameterShouldReturnTrueWhenIsValueTrueMixedCase() {
        assertThat(getParameterValued("trUE", false)).isTrue();
    }

    @Test
    public void getInitParameterShouldReturnFalseWhenIsValueFalseLowerCase() {
        assertThat(getParameterValued("false", true)).isFalse();
    }

    @Test
    public void getInitParameterShouldReturnFalseWhenIsValueFalseUpperCase() {
        assertThat(getParameterValued("FALSE", true)).isFalse();
    }

    @Test
    public void getInitParameterShouldReturnFalseWhenIsValueFalseMixedCase() {
        assertThat(getParameterValued("fALSe", true)).isFalse();
    }

    @Test
    public void getInitParameterShouldReturnDefaultValueAsTrueWhenBadValue() {
        assertThat(getParameterValued("fals", true)).isTrue();
        assertThat(getParameterValued("TRU", true)).isTrue();
        assertThat(getParameterValued("FALSEest", true)).isTrue();
        assertThat(getParameterValued("", true)).isTrue();
        assertThat(getParameterValued("gubbins", true)).isTrue();
    }

    @Test
    public void getInitParameterShouldReturnDefaultValueAsFalseWhenBadValue() {
        assertThat(getParameterValued("fals", false)).isFalse();
        assertThat(getParameterValued("TRU", false)).isFalse();
        assertThat(getParameterValued("FALSEest", false)).isFalse();
        assertThat(getParameterValued("", false)).isFalse();
        assertThat(getParameterValued("gubbins", false)).isFalse();
    }

    @Test
    public void getInitParameterShouldReturnAbsentWhenNull() {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
                .build();
        assertThat(MailetUtil.getInitParameter(mailetConfig, A_PARAMETER)).isEmpty();
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldThrowOnEmptyString() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .parse(""))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldThrowOnNull() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .parse(null))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldThrowOnInvalid() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .parse("invalid"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldThrowOnNegativeValue() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .parse("-1"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldThrowOnZero() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .parse("0"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyShouldReturnValue() throws Exception {
        assertThat(MailetUtil.integerConditionParser()
            .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
            .withDefaultValue(DEFAULT_VALUE)
            .parse("1"))
            .isEqualTo(1);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldReturnDefaultOnEmpty() throws Exception {
        assertThat(MailetUtil.integerConditionParser()
            .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
            .withDefaultValue(DEFAULT_VALUE)
            .parse(""))
            .isEqualTo(DEFAULT_VALUE);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldReturnDefaultOnNull() throws Exception {
        assertThat(MailetUtil.integerConditionParser()
            .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
            .withDefaultValue(DEFAULT_VALUE)
            .parse(null))
            .isEqualTo(DEFAULT_VALUE);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldThrowOnInvalidValue() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .withDefaultValue(DEFAULT_VALUE)
                .parse("invalid"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldThrowOnNegativeValue() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .withDefaultValue(DEFAULT_VALUE)
                .parse("-1"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldThrowOnZero() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
                .withDefaultValue(DEFAULT_VALUE)
                .parse("0"))
            .isInstanceOf(MessagingException.class);
    }

    @Test
    public void parseWithStrictlyPositivePolicyAndDefaultValueShouldReturnValue() throws Exception {
        assertThat(MailetUtil.integerConditionParser()
            .withValidationPolicy(MailetUtil.ValidationPolicy.STRICTLY_POSITIVE)
            .withDefaultValue(DEFAULT_VALUE)
            .parse("1"))
            .isEqualTo(1);
    }

    @Test
    public void parseWithPositivePolicyShouldAllowToReturnZero() throws Exception {
        assertThat(MailetUtil.integerConditionParser()
            .withValidationPolicy(MailetUtil.ValidationPolicy.POSITIVE)
            .withDefaultValue(DEFAULT_VALUE)
            .parse("0"))
            .isEqualTo(0);
    }

    @Test
    public void parseWithPortValidationShouldThrowWhenTooBig() throws Exception {
        assertThatThrownBy(
            () -> MailetUtil.integerConditionParser()
                .withValidationPolicy(MailetUtil.ValidationPolicy.VALID_PORT)
                .withDefaultValue(DEFAULT_VALUE)
                .parse(String.valueOf(Port.MAX_PORT_VALUE + 1)))
            .isInstanceOf(MessagingException.class);
    }

    private boolean getParameterValued(String value, boolean defaultValue) {
        FakeMailetConfig mailetConfig = FakeMailetConfig.builder()
            .setProperty(A_PARAMETER, value)
            .build();
        return MailetUtil.getInitParameter(mailetConfig, A_PARAMETER).orElse(defaultValue);
    }
}
