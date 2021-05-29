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
import static io.xlate.yamljson.YamlTestHelper.testEachVersion;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;

class YamlReaderTest {

    enum ReadType {
        STRUCTURE,
        OBJECT,
        ARRAY,
        SCALAR
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    void assertUnreadable(String version, String inputValue, Consumer<JsonReader> reader) throws IOException {
        try (Reader s = new StringReader(inputValue); JsonReader r = createReader(version, s)) {
            assertThrows(JsonParsingException.class, () -> reader.accept(r));
        }
    }

    JsonValue assertReadable(String version, String inputValue, Function<JsonReader, JsonValue> reader) throws IOException {
        try (Reader s = new StringReader(inputValue); JsonReader r = createReader(version, s)) {
            return reader.apply(r);
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testReadObject(String version) throws IOException {
        JsonObject object = null;

        try (InputStream source = getClass().getResourceAsStream("/simpleapi.yaml");
                JsonReader reader = createReader(version, source)) {
            object = reader.readObject();
            assertThrows(IllegalStateException.class, () -> reader.read());
        }

        assertNotNull(object);
    }

    @ParameterizedTest
    @CsvSource({
        "Test scalar, ---%nsimpleValue, SCALAR, jakarta.json.JsonString",
        "Test array , ---%n- v1%n- v2, ARRAY, jakarta.json.JsonArray",
        "Test object, ---%nkey1: value1%nkey2: value2, OBJECT, jakarta.json.JsonObject",
    })
    void testReadTypes(String label, String inputValue, ReadType readType, Class<?> expectedType) throws IOException {
        String formattedInputValue = String.format(inputValue);

        testEachVersion(version -> {
            JsonValue value = null;
            switch (readType) {
            case STRUCTURE:
                value = assertReadable(version, formattedInputValue, JsonReader::read);
                break;

            case OBJECT:
                assertUnreadable(version, formattedInputValue, JsonReader::readArray);

                assertReadable(version, formattedInputValue, JsonReader::read);
                value = assertReadable(version, formattedInputValue, JsonReader::readObject);
                break;

            case ARRAY:
                assertUnreadable(version, formattedInputValue, JsonReader::readObject);

                assertReadable(version, formattedInputValue, JsonReader::read);
                value = assertReadable(version, formattedInputValue, JsonReader::readArray);
                break;

            case SCALAR:
                assertUnreadable(version, formattedInputValue, JsonReader::read);
                assertUnreadable(version, formattedInputValue, JsonReader::readArray);
                assertUnreadable(version, formattedInputValue, JsonReader::readObject);

                value = assertReadable(version, formattedInputValue, JsonReader::readValue);
                break;
            }

            assertTrue(expectedType.isAssignableFrom(value.getClass()));
        });
    }
}
