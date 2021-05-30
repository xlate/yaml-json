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
import static io.xlate.yamljson.YamlTestHelper.testEachVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import jakarta.json.JsonException;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.json.stream.JsonParsingException;

class YamlParserTest {

    JsonParser createParser(String version, Reader reader) {
        return Yaml.createParserFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createParser(reader);
    }

    JsonParser createParser(String version, InputStream stream) {
        return createParser(version, new InputStreamReader(stream));
    }

    Event seekEvent(JsonParser parser, int nextCalls) {
        Event event = null;
        int called = 0;

        for (int i = 0; i < nextCalls && parser.hasNext(); i++) {
            event = parser.next();
            called++;
        }

        assertEquals(nextCalls, called);

        return event;
    }

    @ParameterizedTest
    @CsvSource({
        "Test octal        , ---%nkey: 0o10%n , 3, true, 8",
        "Test hexadecimal  , ---%nkey: 0x10%n , 3, true, 16",
        "Test integer      , ---%nkey: 10%n   , 3, true, 10",
        "Test integer (+)  , ---%nkey: +10%n  , 3, true, 10",
        "Test integer (-)  , ---%nkey: -10%n  , 3, true, -10",
        "Test simple float , ---%nkey: 1.0%n  , 3, false, 1.0",
        "Test leading float, ---%nkey: .101%n  , 3, false, 0.101",
        "Test (E)xp float  , ---%nkey: -1E-5%n, 3, false, -0.00001",
        "Test (e)xp float  , ---%nkey: -1e-5%n, 3, false, -0.00001",
    })
    void testFiniteNumbers(String label, String yaml, int nextCalls, boolean integral, BigDecimal expected) {
        testEachVersion(version -> {
            try (JsonParser parser = createParser(version, new StringReader(String.format(yaml)))) {
                Event event = seekEvent(parser, nextCalls);
                assertEquals(Event.VALUE_NUMBER, event);

                // It's a number
                assertFalse(((AbstractYamlParser<?, ?>) parser).isNaN());
                // It's not infinite
                assertFalse(((AbstractYamlParser<?, ?>) parser).isInfinite());

                assertEquals(integral, parser.isIntegralNumber());

                BigDecimal actual = parser.getBigDecimal();

                assertEquals(expected, actual);
                assertEquals(expected.longValue(), parser.getLong());
                assertEquals(expected.intValue(), parser.getInt());
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "Test infinity    , ---%nkey: .inf%n , 3, .inf",
        "Test infinity (+), ---%nkey: +.Inf%n, 3, +.Inf",
        "Test infinity (-), ---%nkey: -.INF%n, 3, -.INF",
    })
    void testInfiniteValues(String label, String yaml, int nextCalls, String expected) {
        testEachVersion(version -> {
            try (JsonParser parser = createParser(version, new StringReader(String.format(yaml)))) {
                Event event = seekEvent(parser, nextCalls);
                assertEquals(Event.VALUE_STRING, event);
                assertTrue(((AbstractYamlParser<?, ?>) parser).isInfinite());
                String actual = parser.getString();
                assertEquals(expected, actual);
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "Test NaN, ---%nkey: .NaN%n, 3, .NaN"
    })
    void testNaNValues(String label, String yaml, int nextCalls, String expected) {
        testEachVersion(version -> {
            try (JsonParser parser = createParser(version, new StringReader(String.format(yaml)))) {
                Event event = seekEvent(parser, nextCalls);
                assertEquals(Event.VALUE_STRING, event);
                assertFalse(((AbstractYamlParser<?, ?>) parser).isInfinite());
                assertTrue(((AbstractYamlParser<?, ?>) parser).isNaN());
                String actual = parser.getString();
                assertEquals(expected, actual);
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "Test True, ---%nkey: True%n , 3, VALUE_TRUE",
        "Test False, ---%nkey: FALSE%n , 3, VALUE_FALSE",
        "Test Null, ---%nkey: null%n , 3, VALUE_NULL",
        "Test Null when empty, ---%nkey: %n , 3, VALUE_NULL"
    })
    void testEventConstantValues(String label, String yaml, int nextCalls, Event expected) {
        testEachVersion(version -> {
            try (JsonParser parser = createParser(version, new StringReader(String.format(yaml)))) {
                Event event = seekEvent(parser, nextCalls);
                assertEquals(expected, event);
                assertThrows(IllegalStateException.class, () -> parser.getString());
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "Test looks like positive number, ---%nkey: +Not a Number%n , 3, +Not a Number",
        "Test looks like negative number, ---%nkey: -000 Not quite a number%n , 3, -000 Not quite a number",
        "Test looks like floating point , ---%nkey: .000 Not a number either!%n , 3, .000 Not a number either!",
        "Test too many exponentials     , ---%nkey: .00e5e6%n , 3, .00e5e6",
        "Test too many signs            , ---%nkey: -.00e-5+6%n , 3, -.00e-5+6",
        "Test too many decimal points   , ---%nkey: -.00.6%n , 3, -.00.6",
        "Test decimal point in exponent , ---%nkey: 1e1.6%n , 3, 1e1.6",
        "Test looks like octal or hex   , ---%nkey: 0Nether hex nor octal%n, 3, 0Nether hex nor octal",
        "Test too short for hexadecimal , ---%nkey: 0x%n, 3, 0x",
        "Test bad hexadecimal characters, ---%nkey: 0x0Z, 3, 0x0Z",
        "Test too short for octal       , ---%nkey: 0o%n, 3, 0o",
        "Test bad octal characters      , ---%nkey: 0o0Z, 3, 0o0Z",
        "Test regular alphabetic string , ---%nkey: A quick brown fox...%n, 3, A quick brown fox...",
        "Test looks like start of null  , ---%nkey: not null%n, 3, not null",
        "Test quoted string             , ---%nkey: 'Value'%n, 3, Value",
        "Test multi-line string         , ---%nkey: >-%n  Value%n  without%n  newlines%n, 3, Value without newlines",
    })
    void testStringValues(String label, String yaml, int nextCalls, String expected) {
        testEachVersion(version -> {
            try (JsonParser parser = createParser(version, new StringReader(String.format(yaml)))) {
                Event event = seekEvent(parser, nextCalls);
                assertEquals(Event.VALUE_STRING, event);
                assertEquals(expected, parser.getString());
                assertThrows(IllegalStateException.class, () -> parser.getBigDecimal());
            }
        });
    }

    @ParameterizedTest
    @CsvSource({
        "Test location at object start, ---%nkey: value%n, 1, 4, 2, 1",
        "Test location at first key   , ---%nkey: value%n, 2, 4, 2, 1",
        "Test location before stream  , ---%nkey: value%n, 0, -1, -1, -1",
    })
    void testLocationValues(String label, String yaml, int nextCalls, long offset, long line, long column) {
        testEachVersion(version -> {
            InputStream source = new ByteArrayInputStream(String.format(yaml).getBytes());

            try (JsonParser parser = createParser(version, source)) {
                for (int i = 0; i < nextCalls; i++) {
                    parser.next();
                }
                JsonLocation location = parser.getLocation();
                assertEquals(offset, location.getStreamOffset());
                assertEquals(line, location.getLineNumber());
                assertEquals(column, location.getColumnNumber());
            }
        });
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testInvalidInputThrowsJsonParsingException(String version) {
        InputStream source = new ByteArrayInputStream(String.format("---%nkey\u0000%n").getBytes());

        try (JsonParser parser = createParser(version, source)) {
            JsonParsingException thrown = assertThrows(JsonParsingException.class, () -> parser.next());
            Throwable cause = thrown.getCause();
            assertNotNull(cause);
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testInfinityCheckOnKeyThrowsException(String version) {
        InputStream source = new ByteArrayInputStream(String.format("---%nkey: value%n").getBytes());

        try (AbstractYamlParser<?, ?> parser = (AbstractYamlParser<?, ?>) createParser(version, source)) {
            parser.next();
            parser.next();
            assertThrows(IllegalStateException.class, () -> parser.isInfinite());
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testIOExceptionThrowsJsonException(String version) throws IOException {
        Reader proxy = Mockito.mock(Reader.class);

        Mockito.doThrow(IOException.class).when(proxy).read(Mockito.any(char[].class), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doThrow(IOException.class).when(proxy).close();
        IOException closeException = null;

        try (JsonParser parser = createParser(version, proxy)) {
            JsonException thrown = assertThrows(JsonException.class, () -> parser.next());
            Throwable cause = thrown.getCause();
            assertNotNull(cause);
        } catch (JsonException e) {
            closeException = (IOException) e.getCause();
        }

        assertNotNull(closeException);
    }

}
