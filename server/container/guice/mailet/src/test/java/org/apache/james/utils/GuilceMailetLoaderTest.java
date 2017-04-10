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

package org.apache.james.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.transport.mailets.AddFooter;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class GuilceMailetLoaderTest {

    public static final FileSystem THROWING_FILE_SYSTEM = new FileSystem() {
        @Override
        public InputStream getResource(String url) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            throw new FileNotFoundException();
        }

        @Override
        public File getBasedir() throws FileNotFoundException {
            throw new NotImplementedException();
        }
    };

    public static final FileSystem CLASSPATH_FILE_SYSTEM = new FileSystem() {
        @Override
        public InputStream getResource(String url) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            return new File(ClassLoader.getSystemResource("recursive/included-jars").getFile());
        }

        @Override
        public File getBasedir() throws FileNotFoundException {
            throw new NotImplementedException();
        }
    };

    public static final FileSystem RECURSIVE_CLASSPATH_FILE_SYSTEM = new FileSystem() {
        @Override
        public InputStream getResource(String url) throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public File getFile(String fileURL) throws FileNotFoundException {
            return new File(ClassLoader.getSystemResource("recursive/").getFile());
        }

        @Override
        public File getBasedir() throws FileNotFoundException {
            throw new NotImplementedException();
        }
    };

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private Injector injector = Guice.createInjector();

    @Test
    public void getMailetShouldLoadClass() throws Exception {
        GuiceMailetLoader guiceMailetLoader = new GuiceMailetLoader(injector, THROWING_FILE_SYSTEM);

        Mailet mailet = guiceMailetLoader.getMailet(FakeMailetConfig.builder()
            .mailetName("AddFooter")
            .mailetContext(FakeMailContext.defaultContext())
            .build());

        assertThat(mailet).isInstanceOf(AddFooter.class);
    }

    @Test
    public void getMailetShouldThrowOnBadType() throws Exception {
        GuiceMailetLoader guiceMailetLoader = new GuiceMailetLoader(injector, THROWING_FILE_SYSTEM);

        expectedException.expect(MessagingException.class);

        guiceMailetLoader.getMailet(FakeMailetConfig.builder()
            .mailetName("org.apache.james.transport.matchers.SizeGreaterThan")
            .mailetContext(FakeMailContext.defaultContext())
            .build());
    }

    @Test
    public void getMailetShouldLoadClassWhenInIncludedJars() throws Exception {
        GuiceMailetLoader guiceMailetLoader = new GuiceMailetLoader(Guice.createInjector(), CLASSPATH_FILE_SYSTEM);

        Mailet mailet = guiceMailetLoader.getMailet(FakeMailetConfig.builder()
            .mailetName("org.apache.transport.mailets.AIMailet")
            .mailetContext(FakeMailContext.defaultContext())
            .build());

        assertThat(mailet.getClass().getCanonicalName())
            .isEqualTo("org.apache.transport.mailets.AIMailet");
    }

    @Test
    public void getMailedShouldShouldRecursivelyIncludeJar() throws Exception {
        GuiceMailetLoader guiceMailetLoader = new GuiceMailetLoader(Guice.createInjector(), RECURSIVE_CLASSPATH_FILE_SYSTEM);

        Mailet mailet = guiceMailetLoader.getMailet(FakeMailetConfig.builder()
            .mailetName("org.apache.transport.mailets.AIMailet")
            .mailetContext(FakeMailContext.defaultContext())
            .build());

        assertThat(mailet.getClass().getCanonicalName())
            .isEqualTo("org.apache.transport.mailets.AIMailet");
    }

    @Test
    public void getMailetShouldThrowOnUnknownMailet() throws Exception {
        GuiceMailetLoader guiceMailetLoader = new GuiceMailetLoader(Guice.createInjector(), CLASSPATH_FILE_SYSTEM);

        expectedException.expect(MessagingException.class);

        guiceMailetLoader.getMailet(FakeMailetConfig.builder()
            .mailetName("org.apache.transport.mailets.Unknown")
            .mailetContext(FakeMailContext.defaultContext())
            .build());
    }

}
