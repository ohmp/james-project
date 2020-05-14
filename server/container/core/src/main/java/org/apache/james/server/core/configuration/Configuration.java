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

package org.apache.james.server.core.configuration;

import java.io.File;
import java.util.Optional;

import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.server.core.JamesServerResourceLoader;
import org.apache.james.server.core.MissingArgumentException;

public interface Configuration {

    String WORKING_DIRECTORY = "working.directory";

    static Basic.Builder builder() {
        return new Basic.Builder();
    }

    class Builder<T extends Builder> {
        protected Optional<String> rootDirectory;
        protected Optional<String> configurationPath;

        protected Builder() {
            rootDirectory = Optional.empty();
            configurationPath = Optional.empty();
        }

        public T workingDirectory(String path) {
            rootDirectory = Optional.of(path);
            return (T) this;
        }

        public T workingDirectory(File file) {
            rootDirectory = Optional.of(file.getAbsolutePath());
            return (T) this;
        }

        public T useWorkingDirectoryEnvProperty() {
            rootDirectory = Optional
                .ofNullable(System.getProperty(WORKING_DIRECTORY));
            if (!rootDirectory.isPresent()) {
                throw new MissingArgumentException("Server needs a working.directory env entry");
            }
            return (T) this;
        }

        public T configurationPath(String path) {
            configurationPath = Optional.of(path);
            return (T) this;
        }

        public T configurationFromClasspath() {
            configurationPath = Optional.of(FileSystem.CLASSPATH_PROTOCOL);
            return (T) this;
        }

        protected JamesServerResourceLoader directories() {
            return new JamesServerResourceLoader(rootDirectory
                .orElseThrow(() -> new MissingArgumentException("Server needs a working.directory env entry")));
        }

        protected String configurationPath() {
            return configurationPath.orElse(FileSystem.FILE_PROTOCOL_AND_CONF);
        }
    }

    class Basic implements Configuration {
        public static class Builder extends Configuration.Builder<Basic.Builder> {
            public Builder() {
                super();
            }

            public Configuration.Basic build() {
                return new Configuration.Basic(
                    configurationPath(),
                    directories());
            }
        }

        private final String configurationPath;
        private final JamesDirectoriesProvider directories;

        public Basic(String configurationPath, JamesDirectoriesProvider directories) {
            this.configurationPath = configurationPath;
            this.directories = directories;
        }

        @Override
        public String configurationPath() {
            return configurationPath;
        }

        @Override
        public JamesDirectoriesProvider directories() {
            return directories;
        }
    }

    String configurationPath();

    JamesDirectoriesProvider directories();
}
