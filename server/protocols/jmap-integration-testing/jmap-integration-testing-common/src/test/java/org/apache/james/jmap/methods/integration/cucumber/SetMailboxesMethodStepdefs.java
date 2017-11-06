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

package org.apache.james.jmap.methods.integration.cucumber;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class SetMailboxesMethodStepdefs {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    public static final Logger LOGGER = LoggerFactory.getLogger(SetMailboxesMethodStepdefs.class);

    private final MainStepdefs mainStepdefs;
    private final UserStepdefs userStepdefs;
    private final HttpClient httpClient;
    private final HashMap<String, String> creationIdToMailboxId;

    @Inject
    private SetMailboxesMethodStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs, HttpClient httpClient) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
        this.httpClient = httpClient;
        this.creationIdToMailboxId = new HashMap<>();
    }

    @Given("^mailbox \"([^\"]*)\" with (\\d+) messages$")
    public void mailboxWithMessages(String mailboxName, int messageCount) throws Throwable {
        mainStepdefs.mailboxProbe.createMailbox("#private", userStepdefs.getConnectedUser(), mailboxName);
        MailboxPath mailboxPath = MailboxPath.forUser(userStepdefs.getConnectedUser(), mailboxName);
        IntStream
            .range(0, messageCount)
            .forEach(Throwing.intConsumer(i -> appendMessage(mailboxPath, i)));
        mainStepdefs.awaitMethod.run();
    }

    private void appendMessage(MailboxPath mailboxPath, int i) throws MailboxException {
        String content = "Subject: test" + i + "\r\n\r\n"
                + "testBody" + i;
        mainStepdefs.mailboxProbe.appendMessage(userStepdefs.getConnectedUser(), mailboxPath,
                new ByteArrayInputStream(content.getBytes()), new Date(), false, new Flags());
    }

    @Given("^\"([^\"]*)\" creates mailbox \"([^\"]*)\" with creation id \"([^\"]*)\"$")
    public void createMailbox(String username, String mailboxName, String creationId) throws Throwable {
        userStepdefs.execWithUser(username, () -> {
            httpClient.post("[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"" + creationId + "\" : {" +
                "          \"name\" : \"" + mailboxName + "\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]");
            mainStepdefs.awaitMethod.run();
            try {
                String mailboxId = httpClient.jsonPath.read("[0][1].created." + creationId + ".id");
                creationIdToMailboxId.put(creationId, mailboxId);
            } catch (Exception e) {
                LOGGER.warn("Could not read mailboxId attached to {}, skipping.", creationId, e);
            }
        });
    }

    @Given("^\"([^\"]*)\" creates mailbox \"([^\"]*)\" with creation id \"([^\"]*)\" in parent mailbox with creation id \"([^\"]*)\"$")
    public void createMailbox(String username, String mailboxName, String creationId, String parentId) throws Throwable {
        String parentMailboxId = creationIdToMailboxId.get(parentId);
        userStepdefs.execWithUser(username, () -> {
            httpClient.post("[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"create\": {" +
                "        \"" + creationId + "\" : {" +
                "          \"name\" : \"" + mailboxName + "\"," +
                "          \"parentId\" : \"" + parentMailboxId + "\"" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]");
            mainStepdefs.awaitMethod.run();
            try {
                String mailboxId = httpClient.jsonPath.read("[0][1].created." + creationId + ".id");
                creationIdToMailboxId.put(creationId, mailboxId);
            } catch (Exception e) {
                LOGGER.warn("Could not read mailboxId attached to {}, skipping.", creationId, e);
            }
        });
    }

    @When("^\"([^\"]*)\" renames mailbox with creation id \"([^\"]*)\" into \"([^\"]*)\"$")
    public void renamingMailboxViaJMAP(String username, String creationId, String newName) throws Exception {
        userStepdefs.execWithUser(username, () -> httpClient.post("[" +
            "  [ \"setMailboxes\"," +
            "    {" +
            "      \"update\": {" +
            "        \"" + creationIdToMailboxId.get(creationId) + "\" : {" +
            "          \"name\" : \"" + newName + "\"" +
            "        }" +
            "      }" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]"));
    }

    @When("^\"([^\"]*)\" moves mailbox with creation id \"([^\"]*)\" as an orphan mailbox$")
    public void moveMailboxViaJMAPAsOrphan(String username, String creationId) throws Exception {
        userStepdefs.execWithUser(username, () -> {
            String requestBody = "[" +
                "  [ \"setMailboxes\"," +
                "    {" +
                "      \"update\": {" +
                "        \"" + creationIdToMailboxId.get(creationId) + "\" : {" +
                "          \"parentId\" : null" +
                "        }" +
                "      }" +
                "    }," +
                "    \"#0\"" +
                "  ]" +
                "]";
            httpClient.post(requestBody);
        });
    }

    @When("^renaming mailbox \"([^\"]*)\" to \"([^\"]*)\"")
    public void renamingMailbox(String actualMailboxName, String newMailboxName) throws Throwable {
        Mailbox mailbox = mainStepdefs.mailboxProbe.getMailbox("#private", userStepdefs.getConnectedUser(), actualMailboxName);
        String mailboxId = mailbox.getMailboxId().serialize();
        String requestBody =
                "[" +
                    "  [ \"setMailboxes\"," +
                    "    {" +
                    "      \"update\": {" +
                    "        \"" + mailboxId + "\" : {" +
                    "          \"name\" : \"" + newMailboxName + "\"" +
                    "        }" +
                    "      }" +
                    "    }," +
                    "    \"#0\"" +
                    "  ]" +
                    "]";
        httpClient.post(requestBody);
    }

    @When("^moving mailbox \"([^\"]*)\" to \"([^\"]*)\"$")
    public void movingMailbox(String actualMailboxPath, String newParentMailboxPath) throws Throwable {
        String username = userStepdefs.getConnectedUser();
        Mailbox mailbox = mainStepdefs.mailboxProbe.getMailbox("#private", username, actualMailboxPath);
        String mailboxId = mailbox.getMailboxId().serialize();
        Mailbox parent = mainStepdefs.mailboxProbe.getMailbox("#private", username, newParentMailboxPath);
        String parentId = parent.getMailboxId().serialize();

        String requestBody =
                "[" +
                    "  [ \"setMailboxes\"," +
                    "    {" +
                    "      \"update\": {" +
                    "        \"" + mailboxId + "\" : {" +
                    "          \"parentId\" : \"" + parentId + "\"" +
                    "        }" +
                    "      }" +
                    "    }," +
                    "    \"#0\"" +
                    "  ]" +
                    "]";
        httpClient.post(requestBody);
    }

    @Then("^mailbox \"([^\"]*)\" contains (\\d+) messages$")
    public void mailboxContainsMessages(String mailboxName, int messageCount) throws Throwable {
        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        String username = userStepdefs.getConnectedUser();
        Mailbox mailbox = mainStepdefs.mailboxProbe.getMailbox("#private", username, mailboxName);
        String mailboxId = mailbox.getMailboxId().serialize();

        Awaitility.await().atMost(Duration.FIVE_SECONDS).pollDelay(slowPacedPollInterval).pollInterval(slowPacedPollInterval).until(() -> {
            String requestBody = "[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + mailboxId + "\"]}}, \"#0\"]]";

            httpClient.post(requestBody);

            assertThat(httpClient.response.getStatusLine().getStatusCode()).isEqualTo(200);
            DocumentContext jsonPath = JsonPath.parse(httpClient.response.getEntity().getContent());
            assertThat(jsonPath.<String>read(NAME)).isEqualTo("messageList");

            return jsonPath.<List<String>>read(ARGUMENTS + ".messageIds").size() == messageCount;
        });
    }

    @Then("^the mailbox with creation id \"([^\"]*)\" is not created$")
    public void assertMailboxNotCreated(String creationId) throws Exception {
        assertThat(httpClient.jsonPath.<Map<String, String>>read("[0][1].notCreated"))
            .containsKeys(creationId);
    }

    @Then("^the mailbox with creation id \"([^\"]*)\" has an error with type \"([^\"]*)\" and a description \"([^\"]*)\"$")
    public void assertMailboxCreationException(String creationId, String type, String description) throws Exception {
        assertThat(httpClient.jsonPath.<String>read("[0][1].notCreated." + creationId + ".type"))
            .isEqualTo(type);
        assertThat(httpClient.jsonPath.<String>read("[0][1].notCreated." + creationId + ".description"))
            .isEqualTo(description);

    }

    @Then("^the mailbox with creation id \"([^\"]*)\" is not updated")
    public void assertMailboxNotUpdated(String creationId) throws Exception {
        assertThat(httpClient.jsonPath.<Map<String, String>>read("[0][1].notUpdated"))
            .containsKeys(creationIdToMailboxId.get(creationId));
    }

    @Then("^the mailbox updated with creation id \"([^\"]*)\" has an error with type \"([^\"]*)\" and a description \"([^\"]*)\"$")
    public void assertMailboxUpdateException(String creationId, String type, String description) throws Exception {
        String mailboxId = creationIdToMailboxId.get(creationId);
        assertThat(httpClient.jsonPath.<String>read("[0][1].notUpdated." + mailboxId + ".type"))
            .isEqualTo(type);
        assertThat(httpClient.jsonPath.<String>read("[0][1].notUpdated." + mailboxId + ".description"))
            .isEqualTo(description);
    }
}
