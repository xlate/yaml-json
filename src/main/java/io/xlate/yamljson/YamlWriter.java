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
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;

class YamlWriter implements JsonWriter {

    final JsonGenerator generator;
    boolean writable = true;

    YamlWriter(JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void writeArray(JsonArray array) {
        write(array);
    }

    @Override
    public void writeObject(JsonObject object) {
        write(object);
    }

    @Override
    public void write(JsonStructure value) {
        write((JsonValue) value);
    }

    @Override
    public void write(JsonValue value) {
        if (!writable) {
            throw new IllegalStateException("writeArray, writeObject, write or close method has already been called");
        }
        generator.write(value);
        writable = false;
    }

    @Override
    public void close() {
        generator.close();
        writable = false;
    }

}
