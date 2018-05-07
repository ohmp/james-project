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

package org.apache.james.mailbox.quota.cassandra;

import java.io.IOException;
import java.util.Optional;

import org.apache.james.eventsourcing.Event;
import org.apache.james.mailbox.quota.mailing.events.QuotaThresholdChangedEvent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;

public class JsonEventSerializer {

    private final ObjectMapper objectMapper;

    private final ImmutableBiMap<String, Class<? extends Event>> eventTypes =
        ImmutableBiMap.<String, Class<? extends Event>>builder()
            .put("quota-threshold-change", QuotaThresholdChangedEvent.class)
            .build();

    public static class EventDeserializer extends StdDeserializer<Event> {

        private final ObjectMapper mapper;
        private final ImmutableMap<String, Class<? extends Event>> eventTypesByKey;

        public EventDeserializer(ObjectMapper mapper, ImmutableMap<String, Class<? extends Event>> eventTypesByKey) {
            super(Event.class);
            this.mapper = mapper;
            this.eventTypesByKey = eventTypesByKey;
        }

        @Override
        public Event deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) parser.getCodec();
            ObjectNode root = mapper.readTree(parser);
            JsonNode type = root.get("type");
            return Optional.ofNullable(eventTypesByKey.get(type.asText()))
                .map(clazz -> {
                    try {
                        return mapper.readValue(parser, clazz);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("unknown event type"));
        }

    }

    public static class WrapperObject {
        private final String type;
        private final Event event;

        public WrapperObject(String type, Event event) {
            this.type = type;
            this.event = event;
        }

        public Event getEvent() {
            return event;
        }

        public String getType() {
            return type;
        }
    }

    public static class EventSerializer extends StdSerializer<Event> {
        private final ObjectMapper mapper;
        private final ImmutableMap<Class<? extends Event>, String> eventKeysByType;

        public EventSerializer(ObjectMapper mapper, ImmutableMap<Class<? extends Event>, String> eventKeysByType) {
            super(Event.class);
            this.mapper = mapper;
            this.eventKeysByType = eventKeysByType;
        }


        @Override
        public void serialize(Event value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStringField("type", eventKeysByType.get(value.getClass()));
            gen.writeFieldName("value");
            gen.writeObject(value);
        }
    }

    public JsonEventSerializer() {
        objectMapper = new ObjectMapper();
        EventDeserializer deserializer = new EventDeserializer(objectMapper, eventTypes);
        EventSerializer serializer = new EventSerializer(objectMapper, eventTypes.inverse());
        SimpleModule module = new SimpleModule("PolymorphicEventDeserializerModule");
        module.addDeserializer(Event.class, deserializer);
        module.addSerializer(Event.class, serializer);
        objectMapper.registerModule(module);
    }

    String serialize(Event event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }

    public Event deserialize(String value) throws IOException {
        return objectMapper.readValue(value, Event.class);
    }

}
