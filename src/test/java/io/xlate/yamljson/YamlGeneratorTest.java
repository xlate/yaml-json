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
import static io.xlate.yamljson.YamlTestHelper.createGenerator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.yaml.snakeyaml.DumperOptions;

@DisabledIfSystemProperty(named = Yaml.Settings.YAML_VERSION, matches = "NONE")
class YamlGeneratorTest {

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSimple(String version) {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = createGenerator(version, writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("\"testKey\": \"testValue\"\n", writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSimpleWithQuotesMinimized(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_MINIMIZE_QUOTES, true));

        try (JsonGenerator generator = createGenerator(config, writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("testKey: testValue\n", writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testWriteKeyAtRootThrowsException(String version) {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = createGenerator(version, writer)) {
            assertThrows(JsonGenerationException.class, () -> generator.writeKey("key"));
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testWriteKeyInArrayThrowsException(String version) {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = createGenerator(version, writer)) {
            generator.writeStartArray();
            assertThrows(JsonGenerationException.class, () -> generator.writeKey("key"));
        } catch (JsonGenerationException jge) {
            // Ignore the exception thrown by implicit call to close()
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSequenceOfValues(String version) {
        StringWriter writer = new StringWriter();
        writeSequenceOfValues(null, version, writer);
        assertEquals("\"values\":\n"
                + "- 3.14\n"
                + "- 1000000\n"
                + "- false\n"
                + "- 2.71\n"
                + "- 2021\n"
                + "- 2022\n"
                + "- \"Just a String\"\n"
                + "- null\n"
                + "- \"This line\\nspans multiple\\nlines\"\n"
                + "- \"Contains both ' and \\\" (quote types)\"\n"
                + "- \"Contains only '\"\n"
                + "- \"Contains only \\\"\"\n"
                + "- \"3.14\"\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSequenceOfValuesWithLiteralBlockStyle(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_LITERAL_BLOCK_STYLE, true));
        writeSequenceOfValues(config, null, writer);
        assertEquals("\"values\":\n"
                + "- 3.14\n"
                + "- 1000000\n"
                + "- false\n"
                + "- 2.71\n"
                + "- 2021\n"
                + "- 2022\n"
                + "- \"Just a String\"\n"
                + "- null\n"
                + "- |-\n  This line\n  spans multiple\n  lines\n"
                + "- \"Contains both ' and \\\" (quote types)\"\n"
                + "- \"Contains only '\"\n"
                + "- \"Contains only \\\"\"\n"
                + "- \"3.14\"\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSequenceOfValuesWithQuotesMinimized(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_MINIMIZE_QUOTES, true));
        writeSequenceOfValues(config, null, writer);
        assertEquals("values:\n"
                + "- 3.14\n"
                + "- 1000000\n"
                + "- false\n"
                + "- 2.71\n"
                + "- 2021\n"
                + "- 2022\n"
                + "- Just a String\n"
                + "- null\n"
                + "- |-\n  This line\n  spans multiple\n  lines\n"
                + "- Contains both ' and \" (quote types)\n"
                + "- Contains only '\n"
                + "- Contains only \"\n"
                + "- \"3.14\"\n",
                writer.toString());
    }

    private void writeSequenceOfValues(Map<String, ?> config, String version, Writer writer) {
        try (JsonGenerator generator = config != null ?
                createGenerator(config, writer) :
                createGenerator(version, writer)) {
            generator.writeStartObject()
                .writeStartArray("values")
                    .write(new BigDecimal("3.14"))
                    .write(BigInteger.valueOf(1_000_000))
                    .write(Boolean.FALSE)
                    .write(2.71d)
                    .write(2_021)
                    .write(2_022L)
                    .write("Just a String")
                    .writeNull()
                    .write("This line\nspans multiple\nlines")
                    .write("Contains both ' and \" (quote types)")
                    .write("Contains only '")
                    .write("Contains only \"")
                    .write("3.14") // string that looks like a number
                .writeEnd()
            .writeEnd();
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testMappingOfValues(String version) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writeMappingOfValues(null, version, stream);
        assertEquals("\"values\":\n"
                + "  \"BigDecimal\": 3.141592653589793238E+19\n"
                + "  \"BigInteger\": 1000000\n"
                + "  \"Boolean\": true\n"
                + "  \"double\": 2.71\n"
                + "  \"int\": 2021\n"
                + "  \"long\": 2022\n"
                + "  \"String\": \"Just a String\"\n"
                + "  \"Null\": null\n"
                + "  \"Multiline\": \"This line\\nspans multiple\\nlines\"\n"
                + "  \"MultilineQuotes\": \"Contains both ' and \\\" (quote types)\"\n"
                + "  \"SingleQuote\": \"Contains only '\"\n"
                + "  \"DoubleQuote\": \"Contains only \\\"\"\n"
                + "  \"100\": \"Numeric key\"\n"
                + "  \"empty\": \"\"\n"
                + "  \"blank\": \" \"\n"
                + "  \"positiveInfinity\": .inf\n"
                + "  \"negativeInfinity\": -.inf\n"
                + "  \"NaN\": .nan\n",
                new String(stream.toByteArray()));
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testMappingOfValuesWithQuotesMinimized(String version) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_MINIMIZE_QUOTES, true));
        writeMappingOfValues(config, null, stream);
        assertEquals("values:\n"
                + "  BigDecimal: 3.141592653589793238E+19\n"
                + "  BigInteger: 1000000\n"
                + "  Boolean: true\n"
                + "  double: 2.71\n"
                + "  int: 2021\n"
                + "  long: 2022\n"
                + "  String: Just a String\n"
                + "  \"Null\": null\n"
                + "  Multiline: |-\n    This line\n    spans multiple\n    lines\n"
                + "  MultilineQuotes: Contains both ' and \" (quote types)\n"
                + "  SingleQuote: Contains only '\n"
                + "  DoubleQuote: Contains only \"\n"
                + "  \"100\": Numeric key\n"
                + "  empty: \"\"\n"
                + "  blank: \" \"\n"
                + "  positiveInfinity: .inf\n"
                + "  negativeInfinity: -.inf\n"
                + "  NaN: .nan\n",
                new String(stream.toByteArray()));
    }

    private void writeMappingOfValues(Map<String, ?> config, String version, OutputStream stream) {
        try (JsonGenerator generator = config != null ?
                createGenerator(config, stream) :
                createGenerator(version, stream)) {
            generator.writeStartObject()
                .writeStartObject("values")
                    .write("BigDecimal", new BigDecimal("3.141592653589793238E19"))
                    .write("BigInteger", BigInteger.valueOf(1_000_000))
                    .write("Boolean", Boolean.TRUE)
                    .write("double", 2.71d)
                    .write("int", 2_021)
                    .write("long", 2_022L)
                    .write("String", "Just a String")
                    .writeNull("Null")
                    .write("Multiline", "This line\nspans multiple\nlines")
                    .write("MultilineQuotes", "Contains both ' and \" (quote types)")
                    .write("SingleQuote", "Contains only '")
                    .write("DoubleQuote", "Contains only \"")
                    .write("100", "Numeric key")
                    .write("empty", "")
                    .write("blank", " ")
                    .write("positiveInfinity", Double.POSITIVE_INFINITY)
                    .write("negativeInfinity", Double.NEGATIVE_INFINITY)
                    .write("NaN", Double.NaN)
                .writeEnd()
            .writeEnd();
        }
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testExplicitDocumentStart(String version) {
        Object config;

        if (Yaml.Versions.V1_1.endsWith(version)) {
            DumperOptions opt = new DumperOptions();
            opt.setExplicitStart(true);
            config = opt;
        } else {
            config = DumpSettings.builder()
                    .setExplicitStart(true)
                    .build();
        }

        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_CONFIG, config,
                                                                          Yaml.Settings.YAML_VERSION, version));
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("---\n\"testKey\": \"testValue\"\n", writer.toString());
    }

    @Deprecated
    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testExplicitDocumentStartWithDeprecatedProperty(String version) {
        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_EXPLICIT_START, "true",
                                                                          Yaml.Settings.YAML_VERSION, version));
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("---\n\"testKey\": \"testValue\"\n", writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testExplicitDocumentEnd(String version) {
        Object config;

        if (Yaml.Versions.V1_1.endsWith(version)) {
            DumperOptions opt = new DumperOptions();
            opt.setExplicitEnd(true);
            config = opt;
        } else {
            config = DumpSettings.builder()
                    .setExplicitEnd(true)
                    .build();
        }

        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_CONFIG, config,
                                                                          Yaml.Settings.YAML_VERSION, version));

        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("\"testKey\": \"testValue\"\n...\n", writer.toString());
    }

    @Deprecated
    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testExplicitDocumentEndWithDeprecatedProperty(String version) {
        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_EXPLICIT_END, "true",
                                                                          Yaml.Settings.YAML_VERSION, version));
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("\"testKey\": \"testValue\"\n...\n", writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSpecialStringsQuoted(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_MINIMIZE_QUOTES, true));

        try (JsonGenerator generator = createGenerator(config, writer)) {
            generator.writeStartObject()
                .write("#keywithhash", "value with:\tcolon")
                .write("#anotherwithhash", "value with:colon but the :is not followed by a space")
                .write("key with spaces", "ends with colon:")
                .write("key\twith\ttabs", "ends with hash (preceded by space) #")
                .write("hash# in the middle", "#hash at the start of the value")
                .write("hash\t# with tab", "value with hash (#) preceded by tab\t#")
                .write(".inf", "Key is infinite")
                .write(".NAN", "Key is not a number!")
                .write("false", "Key is reserved word")
                .write("array[]", "Key has indicators, but not inside of a flow collection")
                .write("? question", "Key has leading indicator followed by space")
                .write("\ttab first", "Key with leading tab (special character) is quoted")
                .writeEnd();

            writer.flush();
        }

        assertEquals(""
                + "\"#keywithhash\": \"value with:\\tcolon\"\n"
                + "\"#anotherwithhash\": value with:colon but the :is not followed by a space\n"
                + "key with spaces: \"ends with colon:\"\n"
                // snakeyaml* adds quotes due to `\t`
                + "\"key\\twith\\ttabs\": \"ends with hash (preceded by space) #\"\n"
                + "hash# in the middle: \"#hash at the start of the value\"\n"
                + "\"hash\\t# with tab\": \"value with hash (#) preceded by tab\\t#\"\n"
                + "\".inf\": Key is infinite\n"
                + "\".NAN\": Key is not a number!\n"
                + "\"false\": Key is reserved word\n"
                + "array[]: Key has indicators, but not inside of a flow collection\n"
                + "\"? question\": Key has leading indicator followed by space\n"
                + "\"\\ttab first\": Key with leading tab (special character) is quoted\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testNumericStringsUnquotedWithConfiguration(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_MINIMIZE_QUOTES, true),
            Map.entry(Yaml.Settings.DUMP_QUOTE_NUMERIC_STRINGS, false));

        try (JsonGenerator generator = createGenerator(config, writer)) {
            generator.writeStartObject()
                .write(".inf", "Key is infinite")
                .write("ValueIsInfinite", "-.INF")
                .write(".NAN", "Key is not a number!")
                .write("ValueIsNotNumber", ".NaN")
                .write("ValueIsNumber", new BigDecimal("3.14").toString())
                .writeEnd();

            writer.flush();
        }

        assertEquals(""
                + "\".inf\": Key is infinite\n"
                + "ValueIsInfinite: -.INF\n"
                + "\".NAN\": Key is not a number!\n"
                + "ValueIsNotNumber: .NaN\n"
                + "ValueIsNumber: 3.14\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testBigDecimalValueSerializedPlain(String version) {
        StringWriter writer = new StringWriter();
        Map<String, ?> config = Map.ofEntries(
            Map.entry(Yaml.Settings.YAML_VERSION, version),
            Map.entry(Yaml.Settings.DUMP_WRITE_PLAIN_BIGDECIMAL, true));

        try (JsonGenerator generator = createGenerator(config, writer)) {
            generator.writeStartObject()
                .write("pi-e19", new BigDecimal("3.141592653589793238E19"))
                .writeEnd();

            writer.flush();
        }

        assertEquals(""
                + "\"pi-e19\": 31415926535897932380\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testWriteEndWithoutContextThrowsException(String version) {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = createGenerator(version, writer)) {
            generator.writeStartObject()
                .write("somekey", "somevalue")
                .writeEnd();

            assertThrows(JsonGenerationException.class, () -> generator.writeEnd());
        }
    }
}
