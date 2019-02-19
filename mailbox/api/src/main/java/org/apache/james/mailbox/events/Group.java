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

package org.apache.james.mailbox.events;

import java.util.Objects;

import com.google.common.base.Preconditions;

public class Group {
    public static Group deserialize(String serializedGroup) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (serializedGroup.startsWith(GenericGroup.class.getName() + GenericGroup.DELIMITER)) {
            return new GenericGroup(serializedGroup.substring(GenericGroup.class.getName().length() + 1));
        }
        return loadGroup(serializedGroup);
    }

    private static Group loadGroup(String serializedGroup) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClassLoader classLoader = Group.class.getClassLoader();
        Class<?> aClass = classLoader.loadClass(serializedGroup);
        return instanciateGroup(aClass);
    }

    private static Group instanciateGroup(Class<?> aClass) throws InstantiationException, IllegalAccessException {
        Preconditions.checkArgument(Group.class.isAssignableFrom(aClass), "The supplied class is not a group: " + aClass.getName());
        return (Group) aClass.newInstance();
    }

    public String asString() {
        return getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        return this.getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
