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

package org.apache.james.container.spring.bean.factorypostprocessor;

import com.google.common.base.Strings;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.container.spring.lifecycle.ConfigurationProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class EventsConfigurationBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ConfigurationProvider confProvider = beanFactory.getBean(ConfigurationProvider.class);
        try {
            HierarchicalConfiguration config = confProvider.getConfiguration("events");
            String type = config.getString("type", "default");
            String serialization = config.getString("serialization", "json");
            String publisher = config.getString("publisher", "kafka");
            String registration = config.getString("registration", "cassandra");
            String delegatingListenerAlias = getDelegatingListenerAlias(type);
            String serializationAlias = getSerializationAlias(serialization);
            String registrationAlias = getRegistrationAlias(registration);
            String publisherAlias = null;
            String consumerAlias = null;

            if (publisher.equals("kafka")) {
                publisherAlias = "kafka-publisher";
                consumerAlias = "kafka-consumer";
            }

            detectInvalidMailboxDelegatingListener(type, delegatingListenerAlias);
            beanFactory.registerAlias(delegatingListenerAlias, "delegating-listener");
            if (!delegatingListenerAlias.equals("default")) {
                detectInvalidSerializationSystem(serializationAlias, "Serialization system type " + serialization + " not supported!");
                beanFactory.registerAlias(serializationAlias, "event-serializer");
                detectInvalidPublisherSystem(publisher, publisherAlias);
                beanFactory.registerAlias(publisherAlias, "publisher");
                beanFactory.registerAlias(consumerAlias, "consumer");
                if (delegatingListenerAlias.equals("registered")) {
                    detectInvalidRegistrationSystem(registration, registrationAlias);
                    beanFactory.registerAlias(registrationAlias, "distant-mailbox-path-register-mapper");
                }
            }

        } catch (ConfigurationException e) {
            throw new FatalBeanException("Unable to config the mailboxmanager", e);
        }
    }

    private void detectInvalidRegistrationSystem(String registration, String registrationAlias) throws ConfigurationException {
        if (Strings.isNullOrEmpty(registrationAlias)) {
            throw new ConfigurationException("Registration system type " + registration + " not supported!");
        }
    }

    private void detectInvalidPublisherSystem(String publisher, String publisherAlias) throws ConfigurationException {
        if (Strings.isNullOrEmpty(publisherAlias)) {
            throw new ConfigurationException("Publisher system type " + publisher + " not supported!");
        }
    }

    private void detectInvalidSerializationSystem(String serializationAlias, String message) throws ConfigurationException {
        if (Strings.isNullOrEmpty(serializationAlias)) {
            throw new ConfigurationException(message);
        }
    }

    private void detectInvalidMailboxDelegatingListener(String type, String delegatingListenerAlias) throws ConfigurationException {
        if (Strings.isNullOrEmpty(delegatingListenerAlias)) {
            throw new ConfigurationException("Delegating listener type " + type + " not supported!");
        }
    }

    private String getRegistrationAlias(String registration) {
        if (registration.equals("cassandra")) {
            return  "cassandra-mailbox-path-register-mapper";
        }
        return null;
    }

    private String getSerializationAlias(String serialization) {
        if (serialization.equals("json")) {
            return "json-event-serializer";
        } else if (serialization.equals("message-pack")) {
            return "message-pack-event-serializer";
        }
        return null;
    }

    private String getDelegatingListenerAlias(String type) {
        if (type.equals("default")) {
            return "default-delegating-listener";
        } else if (type.equals("broadcast")) {
            return "broadcast-delegating-listener";
        } else if (type.equals("registered")) {
            return "registered-delegating-listener";
        }
        return null;
    }
}
