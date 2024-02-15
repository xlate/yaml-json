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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

class YamlReader implements JsonReader {

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
        return new jakarta.json.stream.JsonParsingException(message, cause, parser.getLocation());
    }

    @Override
    public JsonValue readValue() {
        assertReadable();
        parser.next();
        JsonValue value = parser.getValue();
        readable = false;
        return value;
    }
}
