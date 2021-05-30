/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.xlate.yamljson;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

class YamlReader implements JsonReader {

    final JsonProvider jsonProvider = JsonProvider.provider();
    final YamlParser<?, ?> parser;
    boolean readable = true;

    YamlReader(YamlParser<?, ?> parser) {
        this.parser = parser;
    }

    @Override
    public JsonStructure read() {
        JsonValue value = readValue();

        if (value instanceof JsonStructure) {
            return (JsonStructure) value;
        }

        throw newJsonParsingException("Expected JsonStructure, but found " + value.getClass(), null);
    }

    @Override
    public JsonObject readObject() {
        JsonStructure value = read();

        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }

        throw newJsonParsingException("Expected JsonObject, but found " + value.getClass(), null);
    }

    @Override
    public JsonArray readArray() {
        JsonStructure value = read();

        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }

        throw newJsonParsingException("Expected JsonArray, but found " + value.getClass(), null);
    }

    @Override
    public void close() {
        parser.close();
        readable = false;
    }

    void assertReadable() {
        if (!readable) {
            throw new IllegalStateException("read, readObject, readArray, or close method has already been called");
        }
    }

    RuntimeException newJsonParsingException(String message, Throwable cause) {
        return new jakarta.json.stream.JsonParsingException(message, cause, parser);
    }

    @Override
    public JsonValue readValue() {
        assertReadable();

        Deque<Object> builders = new ArrayDeque<>();
        Deque<String> keyNames = new LinkedList<>();
        Object rootBuilder = null;
        String keyName = null;
        JsonValue rootValue = null;

        while (parser.hasNext()) {
            Event event = parser.next();

            switch (event) {
            case KEY_NAME:
                keyName = parser.getString();
                break;
            case START_ARRAY:
                rootBuilder = beginStructure(builders, keyNames, keyName, Json.createArrayBuilder());
                keyName = null;
                break;
            case START_OBJECT:
                rootBuilder = beginStructure(builders, keyNames, keyName, Json.createObjectBuilder());
                keyName = null;
                break;
            case END_ARRAY:
            case END_OBJECT:
                endStructure(builders, keyNames);
                break;
            case VALUE_FALSE:
            case VALUE_NULL:
            case VALUE_NUMBER:
            case VALUE_STRING:
            case VALUE_TRUE:
                JsonValue parsedValue = getParsedValue(event, parser);

                if (builders.isEmpty()) {
                    rootValue = parsedValue;
                } else {
                    addValue(builders.peekLast(), keyName, parsedValue);
                }

                keyName = null;
                break;

            default:
                break;
            }
        }

        readable = false;

        if (rootBuilder != null) {
            rootValue = build(rootBuilder);
        }

        return rootValue;
    }

    Object beginStructure(Deque<Object> builders, Deque<String> keyNames, String keyName, Object builder) {
        builders.add(builder);
        keyNames.push(keyName);

        return builders.peek();
    }

    void endStructure(Deque<Object> builders, Deque<String> keyNames) {
        final Object completedStructure = builders.removeLast();

        if (builders.isEmpty()) {
            // Nothing to do, this is the top level builder
        } else {
            final String keyName = keyNames.pop();
            final JsonValue value = build(completedStructure);
            final Object parentBuilder = builders.peekLast();

            addValue(parentBuilder, keyName, value);
        }
    }

    JsonValue build(Object builder) {
        final JsonValue value;

        if (builder instanceof JsonObjectBuilder) {
            value = ((JsonObjectBuilder) builder).build();
        } else {
            value = ((JsonArrayBuilder) builder).build();
        }

        return value;
    }

    JsonValue getParsedValue(Event event, JsonParser parser) {
        JsonValue value;

        switch (event) {
        case VALUE_TRUE:
            value = JsonValue.TRUE;
            break;
        case VALUE_FALSE:
            value = JsonValue.FALSE;
            break;
        case VALUE_NULL:
            value = JsonValue.NULL;
            break;
        case VALUE_NUMBER:
            value = jsonProvider.createValue(parser.getBigDecimal());
            break;
        case VALUE_STRING:
            value = jsonProvider.createValue(parser.getString());
            break;
        default:
            throw new IllegalStateException("Non-value event: " + event);
        }

        return value;
    }

    void addValue(Object builder, String keyName, JsonValue value) {
        if (keyName != null) {
            ((JsonObjectBuilder) builder).add(keyName, value);
        } else {
            ((JsonArrayBuilder) builder).add(value);
        }
    }
}
