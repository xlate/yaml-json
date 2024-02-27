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
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.yaml.snakeyaml.DumperOptions;

import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

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

        try (JsonGenerator generator = createGenerator(version, writer)) {
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
                .writeEnd()
            .writeEnd();
        }

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
                + "- |-\n  Contains both ' and \" (quote types)\n"
                + "- \"Contains only '\"\n"
                + "- 'Contains only \"'\n",
                writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testMappingOfValues(String version) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (JsonGenerator generator = createGenerator(version, stream)) {
            generator.writeStartObject()
                .writeStartObject("values")
                    .write("BigDecimal", new BigDecimal("3.14"))
                    .write("BigInteger", BigInteger.valueOf(1_000_000))
                    .write("Boolean", Boolean.TRUE)
                    .write("double", 2.71d)
                    .write("int", 2_021)
                    .write("long", 2_022L)
                    .write("String", "Just a String")
                    .writeNull("Null")
                    .write("Multiline", "This line\nspans multiple\nlines")
                    .write("MultilineQuotes", "Contains both ' and \" (quote types)")
                    .write("DoubleQuoted", "Contains only '")
                    .write("SingleQuoted", "Contains only \"")
                    .write("100", "Numeric key")
                    .write("empty", "")
                    .write("blank", " ")
                    .write("positiveInfinity", Double.POSITIVE_INFINITY)
                    .write("negativeInfinity", Double.NEGATIVE_INFINITY)
                    .write("NaN", Double.NaN)
                .writeEnd()
            .writeEnd();
        }

        assertEquals("values:\n"
                + "  BigDecimal: 3.14\n"
                + "  BigInteger: 1000000\n"
                + "  Boolean: true\n"
                + "  double: 2.71\n"
                + "  int: 2021\n"
                + "  long: 2022\n"
                + "  String: Just a String\n"
                + "  'Null': null\n"
                + "  Multiline: |-\n    This line\n    spans multiple\n    lines\n"
                + "  MultilineQuotes: |-\n    Contains both ' and \" (quote types)\n"
                + "  DoubleQuoted: \"Contains only '\"\n"
                + "  SingleQuoted: 'Contains only \"'\n"
                + "  '100': Numeric key\n"
                + "  empty: ''\n"
                + "  blank: ' '\n"
                + "  positiveInfinity: .inf\n"
                + "  negativeInfinity: -.inf\n"
                + "  NaN: .nan\n",
                new String(stream.toByteArray()));
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

        assertEquals("---\ntestKey: testValue\n", writer.toString());
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

        assertEquals("---\ntestKey: testValue\n", writer.toString());
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

        assertEquals("testKey: testValue\n...\n", writer.toString());
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

        assertEquals("testKey: testValue\n...\n", writer.toString());
    }

    @ParameterizedTest
    @MethodSource(VERSIONS_SOURCE)
    void testSpecialStringsQuoted(String version) {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = createGenerator(version, writer)) {
            generator.writeStartObject()
                .write("#keywithhash", "value with: colon")
                .write("#anotherwithhash", "value with:colon but the :is not followed by a space")
                .write("key with spaces", "ends with colon:")
                .write("key\twith\ttabs", "ends with hash #")
                .write("hash# in the middle", "#hash at the start of the value")
                .write("hash\t# with tab", "value with hash (#) preceded by tab\t#")
                .writeEnd();

            writer.flush();
        }

        assertEquals(""
                + "'#keywithhash': 'value with: colon'\n"
                + "'#anotherwithhash': value with:colon but the :is not followed by a space\n"
                + "key with spaces: 'ends with colon:'\n"
                + "\"key\\twith\\ttabs\": 'ends with hash #'\n"
                + "hash# in the middle: '#hash at the start of the value'\n"
                + "\"hash\\t# with tab\": \"value with hash (#) preceded by tab\\t#\"\n",
                writer.toString());
    }
}
