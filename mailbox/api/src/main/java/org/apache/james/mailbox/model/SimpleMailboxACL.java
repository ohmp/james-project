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

package org.apache.james.mailbox.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.james.mailbox.exception.UnsupportedRightException;

import com.github.fge.lambdas.Throwing;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Default implementation of {@link MailboxACL}.
 * 
 */
public class SimpleMailboxACL implements MailboxACL {

    public enum Right implements MailboxACLRight {
        Administer('a'), // (perform SETACL/DELETEACL/GETACL/LISTRIGHTS)
        PerformExpunge('e'), //perform EXPUNGE and expunge as a part of CLOSE
        Insert('i'), //insert (perform APPEND, COPY into mailbox)
        /*
        * create mailboxes (CREATE new sub-mailboxes in any
        * implementation-defined hierarchy, parent mailbox for the new mailbox
        * name in RENAME)
        * */
        CreateMailbox('k'),
        Lookup('l'), //lookup (mailbox is visible to LIST/LSUB commands, SUBSCRIBE mailbox)
        Post('p'), //post (send mail to submission address for mailbox, not enforced by IMAP4 itself)
        Read('r'), //read (SELECT the mailbox, perform STATUS)
        /**
         * keep seen/unseen information across sessions (set or clear \SEEN
         * flag via STORE, also set \SEEN during APPEND/COPY/ FETCH BODY[...])
         */
        WriteSeenFlag('s'),
        DeleteMessages('t'), //delete messages (set or clear \DELETED flag via STORE, set \DELETED flag during APPEND/COPY)
        /**
         * write (set or clear flags other than \SEEN and \DELETED via
         * STORE, also set them during APPEND/COPY)
         */
        Write('w'),
        DeleteMailbox('x'); //delete mailbox (DELETE mailbox, old mailbox name in RENAME)

        private final char rightCharacter;

        Right(char rightCharacter) {
            this.rightCharacter = rightCharacter;
        }

        @Override
        public char asCharacter() {
            return rightCharacter;
        }

        public static final EnumSet<Right> allRights = EnumSet.allOf(Right.class);

        public static Right forChar(char c) throws UnsupportedRightException {
            return Right.allRights
                .stream()
                .filter(r -> r.asCharacter() == c)
                .findFirst()
                .orElseThrow(() -> new UnsupportedRightException(c));
        }
    }

    /**
     * Supports only the Standard Rights of RFC 4314 section 2.1. The rights are
     * stored as single bits in 32 bit int {@link #value} field.
     */
    public static class Rfc4314Rights implements MailboxACLRights {
        /**
         * See RFC 4314 section 2.1.1. Obsolete Rights.
         */
        public enum CompatibilityMode {
            ck_detx, ckx_det, NO_COMPATIBILITY
        }

        private static final char c_ObsoleteCreate = 'c';
        private static final char d_ObsoleteDelete = 'd';

        /**
         * See RFC 4314 section 2.1.1. Obsolete Rights.
         */
        private final CompatibilityMode compatibilityMode = CompatibilityMode.ckx_det;

        private final EnumSet<Right> value;

        private Rfc4314Rights(EnumSet<Right> rights) {
            this.value = EnumSet.copyOf(rights);
        }

        private Rfc4314Rights() {
            this(EnumSet.noneOf(Right.class));
        }

        public Rfc4314Rights(Right... rights) {
            this(EnumSet.copyOf(Arrays.asList(rights)));
        }

        public Rfc4314Rights(MailboxACLRight right) throws UnsupportedRightException {
            this.value = EnumSet.of(Right.forChar(right.asCharacter()));
        }

        /* Used for json serialization (probably a bad idea) */
        public Rfc4314Rights(int serializedRights) {
            List<Right> rights = Right.allRights.stream()
                .filter(right -> ((serializedRights >> right.ordinal()) & 1) != 0)
                .collect(Collectors.toList());
            if (rights.isEmpty()) {
                this.value = EnumSet.noneOf(Right.class);
            } else {
                this.value = EnumSet.copyOf(rights);
            }
        }

        public Rfc4314Rights(String serializedRfc4314Rights) throws UnsupportedRightException {
            List<Right> rights = serializedRfc4314Rights.chars()
                .mapToObj(i -> (char) i)
                .flatMap(Throwing.function(this::convert).sneakyThrow())
                .collect(Collectors.toList());
            if (rights.isEmpty()) {
                this.value = EnumSet.noneOf(Right.class);
            } else {
                this.value = EnumSet.copyOf(rights);
            }
        }

        private Stream<Right> convert(char flag) throws UnsupportedRightException {
            switch (flag) {
            case c_ObsoleteCreate:
                return convertObsoleteCreate(flag);
            case d_ObsoleteDelete:
                return convertObsoleteDelete(flag);
            default:
                return Stream.of(Right.forChar(flag));
            }
        }

        private Stream<Right> convertObsoleteDelete(char flag) throws UnsupportedRightException {
            switch (compatibilityMode) {
            case ck_detx:
                return Stream.of(Right.PerformExpunge, Right.DeleteMessages, Right.DeleteMailbox);
            case ckx_det:
                return Stream.of(Right.PerformExpunge, Right.DeleteMessages);
            case NO_COMPATIBILITY:
                throw new UnsupportedRightException(flag);
            default:
                throw new IllegalStateException("Unexpected enum member: " + CompatibilityMode.class.getName() + "." + compatibilityMode.name());
            }
        }

        private Stream<Right> convertObsoleteCreate(char flag) throws UnsupportedRightException {
            switch (compatibilityMode) {
            case ck_detx:
                return Stream.of(Right.CreateMailbox);
            case ckx_det:
                return Stream.of(Right.CreateMailbox, Right.DeleteMailbox);
            case NO_COMPATIBILITY:
                throw new UnsupportedRightException(flag);
            default:
                throw new IllegalStateException("Unexpected enum member: " + CompatibilityMode.class.getName() + "." + compatibilityMode.name());
            }
        }

        public boolean contains(char flag) throws UnsupportedRightException {
            return contains(Right.forChar(flag));
        }

        @Override
        public boolean contains(MailboxACLRight right) throws UnsupportedRightException {
            return value.contains(right);
        }

        /* Used for json serialization (probably a bad idea) */
        public int serializeAsInteger() {
            return value.stream().mapToInt(x -> 1 << x.ordinal()).sum();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Rfc4314Rights) {
                return this.value.equals(((Rfc4314Rights) o).value);
            } else if (o instanceof MailboxACLRights) {
                try {
                    return this.value == new Rfc4314Rights(((MailboxACLRights) o).serialize()).value;
                } catch (UnsupportedRightException e) {
                    throw new RuntimeException(e);
                }
            }
            return false;
        }

        @Override
        public MailboxACLRights except(MailboxACLRights toRemove) throws UnsupportedRightException {
            EnumSet<Right> copy = EnumSet.copyOf(value);
            copy.removeAll(convertRightsToList(toRemove));
            return new Rfc4314Rights(copy);
        }

        @Override
        public boolean isEmpty() {
            return value.isEmpty();
        }

        @Override
        public boolean isSupported(MailboxACLRight right) {
            try {
                contains(right.asCharacter());
                return true;
            } catch (UnsupportedRightException e) {
                return false;
            }
        }

        @Override
        public Iterator<MailboxACLRight> iterator() {
            ImmutableList<MailboxACLRight> rights = ImmutableList.copyOf(value);
            return rights.iterator();
        }

        @Override
        public String serialize() {
            return value.stream().map(Right::asCharacter).map(String::valueOf).collect(Collectors.joining());
        }

        @Override
        public String toString() {
            return serialize();
        }

        @Override
        public MailboxACLRights union(MailboxACLRights toAdd) throws UnsupportedRightException {
            Preconditions.checkNotNull(toAdd);
            List<Right> rights = convertRightsToList(toAdd);
            EnumSet<Right> copy = EnumSet.copyOf(value);
            copy.addAll(rights);
            return new Rfc4314Rights(copy);
        }

        private List<Right> convertRightsToList(MailboxACLRights toAdd) {
            return ImmutableList.copyOf(Optional.ofNullable(toAdd).orElse(Rfc4314Rights.empty()))
                .stream()
                .map(Throwing.function(right -> Right.forChar(right.asCharacter())))
                .collect(Collectors.toList());
        }

        private static MailboxACLRights empty() {
            return new Rfc4314Rights();
        }

    }

    /**
     * A utility implementation of
     * {@code Map.Entry<MailboxACLEntryKey, MailboxACLRights>}.
     */
    public static class SimpleMailboxACLEntry implements Map.Entry<MailboxACLEntryKey, MailboxACLRights> {
        private final MailboxACLEntryKey key;

        private final MailboxACLRights value;

        public SimpleMailboxACLEntry(MailboxACLEntryKey key, MailboxACLRights value) {
            super();
            this.key = key;
            this.value = value;
        }
        public SimpleMailboxACLEntry(String key, String value) throws UnsupportedRightException {
            this(new SimpleMailboxACLEntryKey(key), new Rfc4314Rights(value));
        }

        @Override
        public MailboxACLEntryKey getKey() {
            return key;
        }

        @Override
        public MailboxACLRights getValue() {
            return value;
        }

        /**
         * Unsupported.
         * 
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        @Override
        public MailboxACLRights setValue(MailboxACLRights value) {
            throw new UnsupportedOperationException("Fields of " + MailboxACLRights.class.getName() + " are read only.");
        }

    }

    /**
     * Default implementation of {@link MailboxACLEntryKey}.
     */
    public static class SimpleMailboxACLEntryKey implements MailboxACLEntryKey {
        public static SimpleMailboxACLEntryKey createGroup(String name) {
            return new SimpleMailboxACLEntryKey(name, NameType.group, false);
        }

        public static SimpleMailboxACLEntryKey createGroup(String name, boolean negative) {
            return new SimpleMailboxACLEntryKey(name, NameType.group, negative);
        }

        public static SimpleMailboxACLEntryKey createUser(String name) {
            return new SimpleMailboxACLEntryKey(name, NameType.user, false);
        }

        public static SimpleMailboxACLEntryKey createUser(String name, boolean negative) {
            return new SimpleMailboxACLEntryKey(name, NameType.user, negative);
        }

        private final String name;
        private final NameType nameType;
        private final boolean negative;

        /**
         * Creates a new instance of SimpleMailboxACLEntryKey from the given
         * serialized {@link String}. It supposes that negative rights are
         * marked with {@link MailboxACL#DEFAULT_NEGATIVE_MARKER} and that
         * groups are marked with {@link MailboxACL#DEFAULT_GROUP_MARKER}.
         * 
         * @param serialized
         */
        public SimpleMailboxACLEntryKey(String serialized) {

            if (serialized == null) {
                throw new IllegalStateException("Cannot parse null to a " + getClass().getName());
            }
            if (serialized.length() == 0) {
                throw new IllegalStateException("Cannot parse an empty string to a " + getClass().getName());
            }
            int start = 0;
            if (serialized.charAt(start) == DEFAULT_NEGATIVE_MARKER) {
                negative = true;
                start++;
            } else {
                negative = false;
            }
            if (serialized.charAt(start) == DEFAULT_GROUP_MARKER) {
                nameType = NameType.group;
                start++;
                name = serialized.substring(start);
                if (name.length() == 0) {
                    throw new IllegalStateException("Cannot parse a string with empty name to a " + getClass().getName());
                }
            } else {
                name = serialized.substring(start);
                if (name.length() == 0) {
                    throw new IllegalStateException("Cannot parse a string with empty name to a " + getClass().getName());
                }
                NameType nt = NameType.user;
                for (SpecialName specialName : SpecialName.values()) {
                    if (specialName.name().equals(name)) {
                        nt = NameType.special;
                        break;
                    }
                }
                this.nameType = nt;
            }
        }

        public SimpleMailboxACLEntryKey(String name, NameType nameType, boolean negative) {
            super();
            if (name == null) {
                throw new NullPointerException("Provide a name for this " + getClass().getName());
            }
            if (nameType == null) {
                throw new NullPointerException("Provide a nameType for this " + getClass().getName());
            }
            this.name = name;
            this.nameType = nameType;
            this.negative = negative;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MailboxACLEntryKey) {
                MailboxACLEntryKey other = (MailboxACLEntryKey) o;
                return Objects.equals(this.name, other.getName())
                    && Objects.equals(this.nameType, other.getNameType())
                    && Objects.equals(this.negative, other.isNegative());
            }
            return false;
        }


        public String getName() {
            return name;
        }

        public NameType getNameType() {
            return nameType;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(negative, nameType, name);
        }

        public boolean isNegative() {
            return negative;
        }

        /**
         * Serializes this {@link SimpleMailboxACLEntryKey} using
         * {@link MailboxACL#DEFAULT_NEGATIVE_MARKER} and
         * {@link MailboxACL#DEFAULT_GROUP_MARKER}.
         * 
         * @see org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey#serialize()
         */
        @Override
        public String serialize() {
            if (!negative) {
                switch (nameType) {
                case special:
                case user:
                    return name;
                case group:
                    return new StringBuilder(name.length() + 1).append(DEFAULT_GROUP_MARKER).append(name).toString();
                default:
                    throw new IllegalStateException();
                }
            } else {
                StringBuilder result = new StringBuilder(name.length() + 2).append(DEFAULT_NEGATIVE_MARKER);
                switch (nameType) {
                case special:
                case user:
                    break;
                case group:
                    result.append(DEFAULT_GROUP_MARKER);
                    break;
                default:
                    throw new IllegalStateException();
                }
                return result.append(name).toString();
            }
        }

        @Override
        public String toString() {
            return serialize();
        }

    }


    public static class SimpleMailboxACLCommand implements MailboxACLCommand {
        private final MailboxACLEntryKey key;
        private final EditMode editMode;
        private final MailboxACLRights rights;

        public SimpleMailboxACLCommand(MailboxACLEntryKey key, EditMode editMode, MailboxACLRights rights) {
            this.key = key;
            this.editMode = editMode;
            this.rights = rights;
        }

        @Override
        public MailboxACLEntryKey getEntryKey() {
            return key;
        }

        @Override
        public EditMode getEditMode() {
            return editMode;
        }

        @Override
        public MailboxACLRights getRights() {
            return rights;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof SimpleMailboxACLCommand) {
                SimpleMailboxACLCommand that = (SimpleMailboxACLCommand) o;

                return Objects.equals(this.key, that.key)
                    && Objects.equals(this.editMode, that.editMode)
                    && Objects.equals(this.rights, that.rights);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(key, editMode, rights);
        }
    }

    public static final MailboxACLEntryKey ANYBODY_KEY;
    public static final MailboxACLEntryKey ANYBODY_NEGATIVE_KEY;
    public static final MailboxACLEntryKey AUTHENTICATED_KEY;
    public static final MailboxACLEntryKey AUTHENTICATED_NEGATIVE_KEY;
    public static final MailboxACL EMPTY;

    public static final MailboxACLRights FULL_RIGHTS;

    public static final MailboxACLRights NO_RIGHTS;
    public static final MailboxACL OWNER_FULL_ACL;
    public static final MailboxACL OWNER_FULL_EXCEPT_ADMINISTRATION_ACL;

    public static final MailboxACLEntryKey OWNER_KEY;
    public static final MailboxACLEntryKey OWNER_NEGATIVE_KEY;

    static {
        try {
            ANYBODY_KEY = new SimpleMailboxACLEntryKey(SpecialName.anybody.name(), NameType.special, false);
            ANYBODY_NEGATIVE_KEY = new SimpleMailboxACLEntryKey(SpecialName.anybody.name(), NameType.special, true);
            AUTHENTICATED_KEY = new SimpleMailboxACLEntryKey(SpecialName.authenticated.name(), NameType.special, false);
            AUTHENTICATED_NEGATIVE_KEY = new SimpleMailboxACLEntryKey(SpecialName.authenticated.name(), NameType.special, true);
            EMPTY = new SimpleMailboxACL();
            FULL_RIGHTS =  new Rfc4314Rights(Right.allRights);
            NO_RIGHTS = new Rfc4314Rights();
            OWNER_KEY = new SimpleMailboxACLEntryKey(SpecialName.owner.name(), NameType.special, false);
            OWNER_NEGATIVE_KEY = new SimpleMailboxACLEntryKey(SpecialName.owner.name(), NameType.special, true);
            OWNER_FULL_ACL = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACL.OWNER_KEY, SimpleMailboxACL.FULL_RIGHTS) });
            OWNER_FULL_EXCEPT_ADMINISTRATION_ACL = new SimpleMailboxACL(new SimpleMailboxACL.SimpleMailboxACLEntry[] { new SimpleMailboxACL.SimpleMailboxACLEntry(SimpleMailboxACL.OWNER_KEY, SimpleMailboxACL.FULL_RIGHTS.except(new Rfc4314Rights(Right.Administer))) });
        } catch (UnsupportedRightException e) {
            throw new RuntimeException(e);
        }
    }
    
    private final Map<MailboxACLEntryKey, MailboxACLRights> entries;

    /**
     * Creates a new instance of SimpleMailboxACL containing no entries.
     * 
     */
    public SimpleMailboxACL() {
        this.entries = Collections.emptyMap();
    }

    /**
     * Creates a new instance of SimpleMailboxACL from the given array of
     * entries.
     * 
     * @param entries
     */
    public SimpleMailboxACL(Map.Entry<MailboxACLEntryKey, MailboxACLRights>[] entries) {
        if (entries != null) {
            Map<MailboxACLEntryKey, MailboxACLRights> m = new HashMap<>(entries.length + entries.length / 2 + 1);
            for (Entry<MailboxACLEntryKey, MailboxACLRights> en : entries) {
                m.put(en.getKey(), en.getValue());
            }
            this.entries = Collections.unmodifiableMap(m);
        } else {
            this.entries = Collections.emptyMap();
        }
    }

    /**
     * Creates a new instance of SimpleMailboxACL from the given {@link Map} of
     * entries.
     * 
     * @param entries
     */
    public SimpleMailboxACL(Map<MailboxACLEntryKey, MailboxACLRights> entries) {
        if (entries != null && entries.size() > 0) {
            Map<MailboxACLEntryKey, MailboxACLRights> m = new HashMap<>(entries.size() + entries.size() / 2 + 1);
            for (Entry<MailboxACLEntryKey, MailboxACLRights> en : entries.entrySet()) {
                m.put(en.getKey(), en.getValue());
            }
            this.entries = Collections.unmodifiableMap(m);
        } else {
            this.entries = Collections.emptyMap();
        }
    }

    /**
     * Creates a new instance of SimpleMailboxACL.
     * <code>unmodifiableEntries</code> parameter is supposed to be umodifiable
     * already.
     * 
     * @param unmodifiableEntries
     * @param dummy
     *            just to be different from {@link #SimpleMailboxACL(Map)}.
     */
    private SimpleMailboxACL(Map<MailboxACLEntryKey, MailboxACLRights> unmodifiableEntries, boolean dummy) {
        this.entries = unmodifiableEntries;
    }

    /**
     * Creates a new instance of SimpleMailboxACL from {@link Properties}. The
     * keys and values from the <code>props</code> parameter are parsed by the
     * {@link String} constructors of {@link SimpleMailboxACLEntryKey} and
     * {@link Rfc4314Rights} respectively.
     * 
     * @param props
     * @throws UnsupportedRightException
     */
    public SimpleMailboxACL(Properties props) throws UnsupportedRightException {
        super();

        Map<MailboxACLEntryKey, MailboxACLRights> m = new HashMap<>(props.size() + props.size() / 2 + 1);

        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            m.put(new SimpleMailboxACLEntryKey((String) prop.getKey()), new Rfc4314Rights((String) prop.getValue()));
        }

        entries = Collections.unmodifiableMap(m);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MailboxACL) {
            MailboxACL acl = (MailboxACL) o;
            return Objects.equals(this.getEntries(), acl.getEntries());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }


    @Override
    public MailboxACL apply(MailboxACLCommand aclUpdate) throws UnsupportedRightException {
        switch (aclUpdate.getEditMode()) {
            case ADD:
                return union(aclUpdate.getEntryKey(), aclUpdate.getRights());
            case REMOVE:
                return except(aclUpdate.getEntryKey(), aclUpdate.getRights());
            case REPLACE:
                return replace(aclUpdate.getEntryKey(), aclUpdate.getRights());
        }
        throw new RuntimeException("Unknown edit mode");
    }

    @Override
    public MailboxACL except(MailboxACL other) throws UnsupportedRightException {
        if (entries.size() == 0) {
            return this;
        } else {
            Map<MailboxACLEntryKey, MailboxACLRights> otherEntries = other.getEntries();
            Map<MailboxACLEntryKey, MailboxACLRights> resultEntries = new HashMap<>(this.entries);
            for (Entry<MailboxACLEntryKey, MailboxACLRights> otherEntry : otherEntries.entrySet()) {
                MailboxACLEntryKey key = otherEntry.getKey();
                MailboxACLRights thisRights = resultEntries.get(key);
                if (thisRights == null) {
                    /* nothing to diff */
                } else {
                    /* diff */
                    MailboxACLRights resultRights = thisRights.except(otherEntry.getValue());
                    if (!resultRights.isEmpty()) {
                        resultEntries.put(key, resultRights);
                    }
                    else {
                        resultEntries.remove(key);
                    }
                }
            }
            return new SimpleMailboxACL(Collections.unmodifiableMap(resultEntries), true);
        }
    }
    
    /**
     * @see org.apache.james.mailbox.model.MailboxACL#except(org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey, org.apache.james.mailbox.model.MailboxACL.MailboxACLRights)
     */
    public MailboxACL except(MailboxACLEntryKey key, MailboxACLRights mailboxACLRights) throws UnsupportedRightException {
        Map<MailboxACLEntryKey, MailboxACLRights> resultEntries = new HashMap<>(this.entries);
        MailboxACLRights thisRights = resultEntries.get(key);
        if (thisRights == null) {
            /* nothing to diff */
        } else {
            /* diff */
            MailboxACLRights resultRights = thisRights.except(mailboxACLRights);
            if (!resultRights.isEmpty()) {
                resultEntries.put(key, resultRights);
            }
            else {
                resultEntries.remove(key);
            }
        }
        return new SimpleMailboxACL(Collections.unmodifiableMap(resultEntries), true);
    }

    @Override
    public Map<MailboxACLEntryKey, MailboxACLRights> getEntries() {
        return entries;
    }

    /**
     * @see org.apache.james.mailbox.model.MailboxACL#replace(org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey, org.apache.james.mailbox.model.MailboxACL.MailboxACLRights)
     */
    @Override
    public MailboxACL replace(MailboxACLEntryKey key, MailboxACLRights replacement) throws UnsupportedRightException {
        Map<MailboxACLEntryKey, MailboxACLRights> resultEntries = new HashMap<>(this.entries);
        if (replacement == null || replacement.isEmpty()) {
            resultEntries.remove(key);
        } else {
            resultEntries.put(key, replacement);
        }
        return new SimpleMailboxACL(Collections.unmodifiableMap(resultEntries), true);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return entries == null ? "" : entries.toString();
    }

    @Override
    public MailboxACL union(MailboxACL other) throws UnsupportedRightException {
        Map<MailboxACLEntryKey, MailboxACLRights> otherEntries = other.getEntries();
        if (otherEntries.size() == 0) {
            return this;
        } else if (entries.size() == 0) {
            return other;
        } else {
            int cnt = otherEntries.size() + entries.size();
            Map<MailboxACLEntryKey, MailboxACLRights> resultEntries = new HashMap<>(cnt + cnt / 2 + 1);
            for (Entry<MailboxACLEntryKey, MailboxACLRights> otherEntry : otherEntries.entrySet()) {
                MailboxACLEntryKey key = otherEntry.getKey();
                MailboxACLRights thisRights = entries.get(key);
                if (thisRights == null) {
                    /* nothing to union */
                    resultEntries.put(key, otherEntry.getValue());
                } else {
                    /* union */
                    resultEntries.put(key, otherEntry.getValue().union(thisRights));
                }
            }
            /* let us check what we have missed in the previous loop */
            for (Entry<MailboxACLEntryKey, MailboxACLRights> thisEntry : entries.entrySet()) {
                MailboxACLEntryKey key = thisEntry.getKey();
                if (!resultEntries.containsKey(key)) {
                    resultEntries.put(key, thisEntry.getValue());
                }
            }
            return new SimpleMailboxACL(Collections.unmodifiableMap(resultEntries), true);
        }
    }
    
    /**
     * @see org.apache.james.mailbox.model.MailboxACL#union(org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey, org.apache.james.mailbox.model.MailboxACL.MailboxACLRights)
     */
    public MailboxACL union(MailboxACLEntryKey key, MailboxACLRights mailboxACLRights) throws UnsupportedRightException {
        Map<MailboxACLEntryKey, MailboxACLRights> resultEntries = new HashMap<>(this.entries);
        MailboxACLRights thisRights = resultEntries.get(key);
        if (thisRights == null) {
            /* nothing to union */
            resultEntries.put(key, mailboxACLRights);
        } else {
            /* union */
            resultEntries.put(key, thisRights.union(mailboxACLRights));
        }
        return new SimpleMailboxACL(Collections.unmodifiableMap(resultEntries), true);
    }

}
