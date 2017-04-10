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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.filesystem.api.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

public class GuiceGenericLoader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceGenericLoader.class);
    public static final String INCLUDED_JARS_FOLDER_NAME = "included-jars/";

    private final Injector injector;
    private final String defaultPackageName;
    private URLClassLoader urlClassLoader;

    public GuiceGenericLoader(Injector injector, FileSystem fileSystem, String defaultPackageName) {
        this.injector = injector;
        this.defaultPackageName = defaultPackageName;

        this.urlClassLoader = new URLClassLoader(retrieveIncludedUrls(fileSystem), getClass().getClassLoader());
    }

    private URL[] retrieveIncludedUrls(FileSystem fileSystem) {
        try {
            File file = fileSystem.getFile("file://" + INCLUDED_JARS_FOLDER_NAME);
            ImmutableList<URL> urls = recursiveExpend(file)
                .collect(Guavate.toImmutableList());
            return urls.toArray(new URL[urls.size()]);
        } catch (IOException e) {
            LOGGER.info("No " + INCLUDED_JARS_FOLDER_NAME + " folder.");
            return new URL[]{};
        }
    }

    private Stream<URL> recursiveExpend(File file) {
        return Optional.ofNullable(file.listFiles())
            .map(Arrays::stream)
            .orElse(Stream.of())
            .flatMap(Throwing.function(this::expendFile).sneakyThrow());
    }

    private Stream<URL> expendFile(File file) throws MalformedURLException {
        if (file.isDirectory()) {
            return recursiveExpend(file);
        }
        LOGGER.info("Loading custom classpath resource " + file.getAbsolutePath());
        return Stream.of(file.toURI().toURL());
    }

    public T instanciate(String className) throws Exception {
        Class<T> clazz = locateClass(className);
        return injector.getInstance(clazz);
    }

    @SuppressWarnings("unchecked")
    private Class<T> locateClass(String className) throws ClassNotFoundException {
        String fullName = constructFullName(className);
        return (Class<T>) urlClassLoader.loadClass(fullName);
    }

    private String constructFullName(String name) {
        if (! name.contains(".")) {
            return defaultPackageName + name;
        }
        return name;
    }

}
