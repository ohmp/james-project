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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.task.Task;
import org.apache.james.task.TaskId;
import org.apache.james.task.TaskManager;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.TaskIdDto;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import spark.Request;
import spark.Response;
import spark.Service;

@Api(tags = "JMAP data operations")
@Path(JmapDataRoutes.BASE_PATH)
@Produces(Constants.JSON_CONTENT_TYPE)
public class JmapDataRoutes implements Routes {
    public static final String BASE_PATH = "/jmap/messages";
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
        service.post(BASE_PATH, this::performJmapDataAction, jsonTransformer);
    }

    @POST
    @Path(BASE_PATH)
    @ApiOperation(value = "Performing operations on JMAP data")
    @ApiImplicitParams({
        @ApiImplicitParam(
            required = true,
            dataType = "String",
            name = "action",
            paramType = "query",
            example = "?action=recomputeJmapPreview",
            value = "Specify the action to perform on JMAP data. For now only 'recomputeJmapPreview' is supported as an action, "
                + "and its purpose is to recompute previews of JMAP messages."),
    })
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.CREATED_201, message = "The taskId of the given scheduled task", response = TaskIdDto.class,
            responseHeaders = {
                @ResponseHeader(name = "Location", description = "URL of the resource associated with the scheduled task")
            }),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Invalid action argument for performing operation on JMAP data"),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "The action requested for performing operation on JMAP data cannot be performed")
    })
    public TaskIdDto performJmapDataAction(Request request, Response response) {
        Preconditions.checkArgument(Objects.equal(request.queryParams(ACTION), RECOMPUTE_JMAP_PREVIEW),
            "'" + ACTION + "' request URL parameter is required. Only '" + RECOMPUTE_JMAP_PREVIEW + "' is supported.");

        Task task = taskFactory.recomputeAllPreviews();
        TaskId taskId = taskManager.submit(task);
        return TaskIdDto.respond(response, taskId);
    }
}
