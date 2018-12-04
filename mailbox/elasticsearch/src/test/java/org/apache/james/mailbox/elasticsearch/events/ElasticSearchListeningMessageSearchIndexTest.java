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
package org.apache.james.mailbox.elasticsearch.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.mail.Flags;

import org.apache.james.backends.es.ElasticSearchIndexer;
import org.apache.james.backends.es.UpdatedRepresentation;
import org.apache.james.core.User;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMailboxSessionMapperFactory;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ElasticSearchListeningMessageSearchIndexTest {
    private static final long MODSEQ = 18L;
    private static final MessageUid MESSAGE_UID = MessageUid.of(1);
    private static final InMemoryId MAILBOX_ID = InMemoryId.of(12);
    private static final String ELASTIC_SEARCH_ID = "12:1";
    private static final String EXPECTED_JSON_CONTENT = "json content";
    private static final String USERNAME = "username";
    public static final int UID_VALIDITY = 42;

    private ElasticSearchIndexer elasticSearchIndexer;
    private MessageToElasticSearchJson messageToElasticSearchJson;
    private ElasticSearchListeningMessageSearchIndex testee;
    private List<User> users;
    private SimpleMailbox mailbox;

    @Before
    public void setup() throws Exception {
        mailbox = new SimpleMailbox(MailboxPath.forUser(USERNAME, "name"), UID_VALIDITY, MAILBOX_ID);
        MailboxSessionMapperFactory mapperFactory = new InMemoryMailboxSessionMapperFactory();
        mapperFactory.getMailboxMapper().save(mailbox);

        messageToElasticSearchJson = mock(MessageToElasticSearchJson.class);
        ElasticSearchSearcher elasticSearchSearcher = mock(ElasticSearchSearcher.class);

        elasticSearchIndexer = mock(ElasticSearchIndexer.class);
        
        testee = new ElasticSearchListeningMessageSearchIndex(mapperFactory, elasticSearchIndexer, elasticSearchSearcher, messageToElasticSearchJson);
        users = ImmutableList.of(User.fromUsername(USERNAME));
    }
    
    @Test
    public void addShouldIndex() throws Exception {
        //Given
        MailboxMessage message = mockedMessage(MESSAGE_UID);
        
        when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
            .thenReturn(EXPECTED_JSON_CONTENT);
        
        //When
        testee.add(mailbox, message);
        
        //Then
        verify(elasticSearchIndexer).index(eq(ELASTIC_SEARCH_ID), eq(EXPECTED_JSON_CONTENT));
    }

    @Test
    public void addShouldIndexEmailBodyWhenNotIndexableAttachment() throws Exception {
        //Given
        MailboxMessage message = mockedMessage(MESSAGE_UID);
        
        when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
            .thenThrow(JsonProcessingException.class);
        
        when(messageToElasticSearchJson.convertToJsonWithoutAttachment(eq(message), eq(users)))
            .thenReturn(EXPECTED_JSON_CONTENT);
        
        //When
        testee.add(mailbox, message);
        
        //Then
        verify(elasticSearchIndexer).index(eq(ELASTIC_SEARCH_ID), eq(EXPECTED_JSON_CONTENT));
    }

    private MailboxMessage mockedMessage(MessageUid messageId) {
        MailboxMessage message = mock(MailboxMessage.class);
        when(message.getUid())
            .thenReturn(messageId);
        when(message.getMailboxId()).thenReturn(MAILBOX_ID);
        return message;
    }

    @Test
    public void addShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
        //Given
        MailboxMessage message = mockedMessage(MESSAGE_UID);
        
        when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
            .thenThrow(JsonProcessingException.class);

        JsonGenerator jsonGenerator = null;
        when(messageToElasticSearchJson.convertToJsonWithoutAttachment(eq(message), eq(users)))
            .thenThrow(new JsonGenerationException("expected error", jsonGenerator));
        
        //When
        testee.add(mailbox, message);
        
        //Then
        //No exception
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldWork() {
        //Given
        BulkResponse expectedBulkResponse = mock(BulkResponse.class);
        when(elasticSearchIndexer.delete(any(List.class)))
            .thenReturn(Optional.of(expectedBulkResponse));

        //When
        testee.delete(mailbox, Lists.newArrayList(MESSAGE_UID));

        //Then
        verify(elasticSearchIndexer).delete(eq(Lists.newArrayList(ELASTIC_SEARCH_ID)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldWorkWhenMultipleMessageIds() {
        //Given
        MessageUid messageId2 = MessageUid.of(2);
        MessageUid messageId3 = MessageUid.of(3);
        MessageUid messageId4 = MessageUid.of(4);
        MessageUid messageId5 = MessageUid.of(5);

        BulkResponse expectedBulkResponse = mock(BulkResponse.class);
        when(elasticSearchIndexer.delete(any(List.class)))
            .thenReturn(Optional.of(expectedBulkResponse));
        
        //When
        testee.delete(mailbox, Lists.newArrayList(MESSAGE_UID, messageId2, messageId3, messageId4, messageId5));
        
        //Then
        verify(elasticSearchIndexer).delete(eq(Lists.newArrayList(ELASTIC_SEARCH_ID, "12:2", "12:3", "12:4", "12:5")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteShouldNotPropagateExceptionWhenExceptionOccurs() {
        //Given
        when(elasticSearchIndexer.delete(any(List.class)))
            .thenThrow(new ElasticsearchException(""));
        
        //When
        testee.delete(mailbox, Lists.newArrayList(MESSAGE_UID));
        
        //Then
        //No exception
    }

    @Test
    public void updateShouldWork() throws Exception {
        //Given
        Flags flags = new Flags();

        UpdatedFlags updatedFlags = UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .modSeq(MODSEQ)
            .oldFlags(flags)
            .newFlags(flags)
            .build();

        when(messageToElasticSearchJson.getUpdatedJsonMessagePart(any(Flags.class), any(Long.class)))
            .thenReturn("json updated content");
        
        //When
        testee.update(mailbox, Lists.newArrayList(updatedFlags));
        
        //Then
        ImmutableList<UpdatedRepresentation> expectedUpdatedRepresentations = ImmutableList.of(new UpdatedRepresentation(ELASTIC_SEARCH_ID, "json updated content"));
        verify(elasticSearchIndexer).update(expectedUpdatedRepresentations);
    }

    @Test
    public void updateShouldNotPropagateExceptionWhenExceptionOccurs() {
        //Given
        Flags flags = new Flags();
        UpdatedFlags updatedFlags = UpdatedFlags.builder()
            .uid(MESSAGE_UID)
            .modSeq(MODSEQ)
            .oldFlags(flags)
            .newFlags(flags)
            .build();

        ImmutableList<UpdatedRepresentation> expectedUpdatedRepresentations = ImmutableList.of(new UpdatedRepresentation(ELASTIC_SEARCH_ID, "json updated content"));
        when(elasticSearchIndexer.update(expectedUpdatedRepresentations))
            .thenThrow(new ElasticsearchException(""));
        
        //When
        testee.update(mailbox, Lists.newArrayList(updatedFlags));
        
        //Then
        //No exception
    }

    @Test
    public void deleteAllShouldWork() {
        //When
        testee.deleteAll(mailbox);
        
        //Then
        QueryBuilder expectedQueryBuilder = QueryBuilders.termQuery("mailboxId", "12");
        verify(elasticSearchIndexer).deleteAllMatchingQuery(refEq(expectedQueryBuilder));
    }

    @Test
    public void deleteAllShouldNotPropagateExceptionWhenExceptionOccurs() {
        //Given
        doThrow(RuntimeException.class)
            .when(elasticSearchIndexer).deleteAllMatchingQuery(QueryBuilders.termQuery("mailboxId", "12"));

        //When
        testee.deleteAll(mailbox);
        
        //Then
        //No Exception
    }

}
