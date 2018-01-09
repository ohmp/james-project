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

import static org.apache.mailet.base.MailetUtil.ValidationPolicy.ALL;

import java.util.Optional;
import java.util.function.Predicate;

import javax.mail.MessagingException;

import org.apache.james.util.OptionalUtils;
import org.apache.james.util.Port;
import org.apache.mailet.MailetConfig;

import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.FunctionChainer;
import com.google.common.base.Strings;


/**
 * Collects utility methods.
 */
public class MailetUtil {

    public static class ValidationPolicy {
        public static final ValidationPolicy ALL = new ValidationPolicy(i -> true, "Not expected message");
        public static final ValidationPolicy STRICTLY_POSITIVE = new ValidationPolicy(i -> i > 0,
            "Expecting condition to be a strictly positive integer.");
        public static final ValidationPolicy POSITIVE = new ValidationPolicy(i -> i >= 0,
            "Expecting condition to be a positive integer.");
        public static final ValidationPolicy VALID_PORT = new ValidationPolicy(Port::isValid,
            "Expecting condition to be a valid port.");

        private final Predicate<Integer> isValidPredicate;
        private final String errorPrefix;

        public ValidationPolicy(Predicate<Integer> isValidPredicate, String errorPrefix) {
            this.isValidPredicate = isValidPredicate;
            this.errorPrefix = errorPrefix;
        }

        public int validate(int value) throws MessagingException {
            if (!isValidPredicate.test(value)) {
                throw new MessagingException(errorPrefix + " Got " + value);
            }
            return value;
        }

        public Optional<Integer> validate(Optional<Integer> value) throws MessagingException {
            FunctionChainer<Integer, Integer> function = Throwing.function(this::validate);
            return value.map(function.sneakyThrow());
        }
    }

    public static class IntegerConditionParser {
        private Optional<Integer> defaultValue;
        private Optional<ValidationPolicy> validationPolicy;

        public IntegerConditionParser() {
            this.defaultValue = Optional.empty();
            this.validationPolicy = Optional.empty();
        }

        public IntegerConditionParser withDefaultValue(int defaultValue) {
            return withDefaultValue(Optional.of(defaultValue));
        }

        public IntegerConditionParser withDefaultValue(Optional<Integer> defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public IntegerConditionParser withValidationPolicy(ValidationPolicy validationPolicy) {
            this.validationPolicy = Optional.of(validationPolicy);
            return this;
        }

        public int parse(String condition) throws MessagingException {
            return parseAsOptional(condition)
                .orElseThrow(() -> new MessagingException("Condition is required. It should be a strictly positive integer"));
        }

        public Optional<Integer> parseAsOptional(String condition) throws MessagingException {
            Optional<String> value = OptionalUtils.or(Optional.ofNullable(condition)
                    .filter(s -> !Strings.isNullOrEmpty(s)),
                defaultValue.map(String::valueOf));

            return validationPolicy.orElse(ALL)
                .validate(tryParseInteger(value));
        }

        private Optional<Integer> tryParseInteger(Optional<String> value) throws MessagingException {
            try {
                return value.map(Integer::valueOf);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
    }

    public static IntegerConditionParser integerConditionParser() {
        return new IntegerConditionParser();
    }

    /**
     * <p>This takes the subject string and reduces (normailzes) it.
     * Multiple "Re:" entries are reduced to one, and capitalized.  The
     * prefix is always moved/placed at the beginning of the line, and
     * extra blanks are reduced, so that the output is always of the
     * form:</p>
     * <code>
     * &lt;prefix&gt; + &lt;one-optional-"Re:"*gt; + &lt;remaining subject&gt;
     * </code>
     * <p>I have done extensive testing of this routine with a standalone
     * driver, and am leaving the commented out debug messages so that
     * when someone decides to enhance this method, it can be yanked it
     * from this file, embedded it with a test driver, and the comments
     * enabled.</p>
     */
    public static String normalizeSubject(String subj, String prefix) {
        StringBuilder subject = new StringBuilder(subj);
        int prefixLength = prefix.length();

        // If the "prefix" is not at the beginning the subject line, remove it
        int index = subject.indexOf(prefix);
        if (index != 0) {

            if (index > 0) {
                subject.delete(index, index + prefixLength);
            }
            subject.insert(0, prefix); // insert prefix at the front
        }

        // Replace Re: with RE:
        String match = "Re:";
        index = subject.indexOf(match, prefixLength);

        while(index > -1) {
            subject.replace(index, index + match.length(), "RE:");
            index = subject.indexOf(match, prefixLength);
        }

        // Reduce them to one at the beginning
        match ="RE:";
        int indexRE = subject.indexOf(match, prefixLength) + match.length();
        index = subject.indexOf(match, indexRE);
        while(index > 0) {    
            subject.delete(index, index + match.length());
            index = subject.indexOf(match, indexRE);
        }

        // Reduce blanks
        match = "  ";
        index = subject.indexOf(match, prefixLength);
        while(index > -1) {
            subject.replace(index, index + match.length(), " ");
            index = subject.indexOf(match, prefixLength);
        }
        return subject.toString();
    }

    
    /**
     * <p>Gets a boolean valued init parameter.</p>
     * @param config not null
     * @param name name of the init parameter to be queried
     * @return true when the init parameter is <code>true</code> (ignoring case);
     * false when the init parameter is <code>false</code> (ignoring case);
     * otherwise the default value
     */
    public static Optional<Boolean> getInitParameter(MailetConfig config, String name) {
        String value = config.getInitParameter(name);
        if ("true".equalsIgnoreCase(value)) {
            return Optional.of(true);
        }
        if ("false".equalsIgnoreCase(value)){
            return Optional.of(false);
        }
        return Optional.empty();
    }

}
