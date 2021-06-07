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

import static io.xlate.yamljson.YamlTestHelper.VERSIONS_SOURCE;
import static io.xlate.yamljson.YamlTestHelper.createReader;
import static io.xlate.yamljson.YamlTestHelper.createWriter;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;

@DisabledIfSystemProperty(named = Yaml.Settings.YAML_VERSION, matches = "NONE")
class YamlWriterTest {

    @BeforeEach
    void setUp() throws Exception {
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testWriteObject(String version) {
        assertDoesNotThrow(() -> {
            StringWriter sink = new StringWriter();

            try (InputStream source = getClass().getResourceAsStream("/simpleapi.yaml");
                 JsonReader reader = createReader(version, source);
                 JsonWriter writer = createWriter(version, sink)) {

                JsonObject value = reader.readObject();
                writer.writeObject(value);
                assertThrows(IllegalStateException.class, () -> writer.write(value));
            }

            System.out.print(sink.toString());
        });
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testScalarStyles(String version) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (JsonWriter writer = createWriter(version, stream)) {
            JsonArray value = Json.createArrayBuilder().add("#http://example.com").build();
            writer.writeArray(value);
            assertThrows(IllegalStateException.class, () -> writer.write(value));
        }

        assertEquals("- '#http://example.com'\n", new String(stream.toByteArray(), StandardCharsets.UTF_8));
    }
}
