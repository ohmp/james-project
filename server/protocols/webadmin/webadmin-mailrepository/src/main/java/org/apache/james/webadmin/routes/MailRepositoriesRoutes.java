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

package org.apache.james.webadmin.routes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.util.streams.Limit;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.service.MailRepositoryStoreService;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;

import com.google.common.base.Strings;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.Request;
import spark.Service;

@Api(tags = "MailRepositories")
@Path("/mailRepositories")
@Produces("application/json")
public class MailRepositoriesRoutes implements Routes {

    public static final String MAIL_REPOSITORIES = "mailRepositories";
    public static final int NO_OFFSET = 0;

    private final JsonTransformer jsonTransformer;
    private final MailRepositoryStoreService repositoryStoreService;
    private Service service;

    @Inject
    public MailRepositoriesRoutes(MailRepositoryStoreService repositoryStoreService, JsonTransformer jsonTransformer) {
        this.repositoryStoreService = repositoryStoreService;
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public void define(Service service) {
        this.service = service;

        defineGetMailRepositories();

        defineListMails();

        defineGetMail();

        defineSize();

        defineDeleteMail();
    }

    @GET
    @Path("/{encodedUrl}")
    @ApiOperation(value = "Listing all mails in a repository")
    @ApiImplicitParams({
        @ApiImplicitParam(
            required = false,
            paramType = "query parameter",
            dataType = "Integer",
            defaultValue = "0",
            example = "?offset=100",
            value = "If present, skips the given number of key in the output."),
        @ApiImplicitParam(
            required = false,
            paramType = "query parameter",
            dataType = "Integer",
            defaultValue = "0",
            example = "?limit=100",
            value = "If present, fixes the maximal number of key returned in that call.")
    })
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.OK_200, message = "The list of all mails in a repository", response = List.class),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side."),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Bad request - invalid parameter")
    })
    public void defineListMails() {
        service.get(MAIL_REPOSITORIES + "/:encodedUrl", (request, response) -> {
            int offset = asInteger(request, "offset").orElse(NO_OFFSET);
            Limit limit = Limit.from(asInteger(request, "limit"));
            String url = URLDecoder.decode(request.params("encodedUrl"), StandardCharsets.UTF_8.displayName());
            try {
                return repositoryStoreService.listMails(url, offset, limit);
            } catch (MailRepositoryStore.MailRepositoryStoreException| MessagingException e) {
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorResponder.ErrorType.SERVER_ERROR)
                    .cause(e)
                    .message("Error while listing keys")
                    .haltError();
            }
        }, jsonTransformer);
    }

    @GET
    @ApiOperation(value = "Listing all mail repository urls")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.OK_200, message = "The list of mail repository urls", response = String.class),
            @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side.")
    })
    public void defineGetMailRepositories() {
        service.get(MAIL_REPOSITORIES, (request, response) -> {
            response.status(HttpStatus.OK_200);
            return repositoryStoreService.listMailRepositories();
        }, jsonTransformer);
    }

    @GET
    @Path("/{encodedUrl}/{mailKey}")
    @ApiOperation(value = "Retrieving a specific mail details")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.OK_200, message = "The list of all mails in a repository", response = List.class),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side."),
        @ApiResponse(code = HttpStatus.NOT_FOUND_404, message = "Not found - Could not retrieve the given mail.")
    })
    public void defineGetMail() {
        service.get(MAIL_REPOSITORIES + "/:encodedUrl/:mailKey", (request, response) -> {
            String url = URLDecoder.decode(request.params("encodedUrl"), StandardCharsets.UTF_8.displayName());
            String mailKey = request.params("mailKey");
            try {
                return repositoryStoreService.retrieveMail(url, mailKey)
                    .orElseThrow(() -> ErrorResponder.builder()
                        .statusCode(HttpStatus.NOT_FOUND_404)
                        .type(ErrorResponder.ErrorType.NOT_FOUND)
                        .message("Could not retrieve " + mailKey)
                        .haltError());
            } catch (MailRepositoryStore.MailRepositoryStoreException| MessagingException e) {
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorResponder.ErrorType.SERVER_ERROR)
                    .cause(e)
                    .message("Error while retrieving mail")
                    .haltError();
            }
        }, jsonTransformer);
    }

    @GET
    @Path("/{encodedUrl}/size")
    @ApiOperation(value = "Reading the size of a repository")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.OK_200, message = "The number of mails in a repository", response = List.class),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side."),
    })
    public void defineSize() {
        service.get(MAIL_REPOSITORIES + "/:encodedUrl/size", (request, response) -> {
            String url = URLDecoder.decode(request.params("encodedUrl"), StandardCharsets.UTF_8.displayName());
            try {
                return repositoryStoreService.size(url);
        } catch (MailRepositoryStore.MailRepositoryStoreException| MessagingException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .type(ErrorResponder.ErrorType.SERVER_ERROR)
                .cause(e)
                .message("Error while retrieving mail repository size")
                .haltError();
            }
        }, jsonTransformer);
    }

    @DELETE
    @Path("/{encodedUrl}/{mailKey}")
    @ApiOperation(value = "Deleting a specific mail from that mailRepository")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.OK_200, message = "Mail is no more stored in the repository", response = List.class),
        @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error - Something went bad on the server side."),
    })
    public void defineDeleteMail() {
        service.delete(MAIL_REPOSITORIES + "/:encodedUrl/:mailKey", (request, response) -> {
            String url = URLDecoder.decode(request.params("encodedUrl"), StandardCharsets.UTF_8.displayName());
            String mailKey = request.params("mailKey");
            try {
                response.status(HttpStatus.NO_CONTENT_204);
                repositoryStoreService.deleteMail(url, mailKey);
                return Constants.EMPTY_BODY;
            } catch (MailRepositoryStore.MailRepositoryStoreException| MessagingException e) {
                throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorResponder.ErrorType.SERVER_ERROR)
                    .cause(e)
                    .message("Error while deleting mail")
                    .haltError();
            }
        }, jsonTransformer);
    }

    private Optional<Integer> asInteger(Request request, String parameterName) {
        try {
            return Optional.ofNullable(request.queryParams(parameterName))
                .filter(s -> !Strings.isNullOrEmpty(s))
                .map(Integer::valueOf)
                .map(value -> keepPositive(value, parameterName));
        } catch (NumberFormatException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .cause(e)
                .message("Can not parse " + parameterName)
                .haltError();
        }
    }

    private int keepPositive(int value, String parameterName) {
        if (value < 0) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message(parameterName + " can not be negative")
                .haltError();
        }
        return value;
    }
}
