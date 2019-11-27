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

package org.apache.james.webadmin.data.jmap;

import javax.inject.Inject;

import org.apache.james.task.Task;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.TaskIdDto;
import org.apache.james.webadmin.utils.JsonTransformer;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import spark.Service;

public class JmapDataRoutes implements Routes {
    private static final String BASE_PATH = "/jmap";
    private static final String ACTION = "action";
    private static final String RECOMPUTE_JMAP_PREVIEW = "recomputeJmapPreview";

    private final TaskManager taskManager;
    private final TaskFactory taskFactory;
    private final JsonTransformer jsonTransformer;

    @Inject
    public JmapDataRoutes(TaskManager taskManager, TaskFactory taskFactory, JsonTransformer jsonTransformer) {
        this.taskManager = taskManager;
        this.taskFactory = taskFactory;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public String getBasePath() {
        return BASE_PATH;
    }

    @Override
    public void define(Service service) {
        service.post(BASE_PATH, ((request, response) -> {
            Preconditions.checkArgument(Objects.equal(request.queryParams(ACTION), RECOMPUTE_JMAP_PREVIEW),
                "'" + ACTION + "' request URL parameter is required. Only '" + RECOMPUTE_JMAP_PREVIEW + "' is supported.");

            Task task = taskFactory.recomputeAllPrevious();
            TaskId taskId = taskManager.submit(task);
            return TaskIdDto.respond(response, taskId);
        }), jsonTransformer);
    }
}
