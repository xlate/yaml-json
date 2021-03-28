package io.xlate.yamljson;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

class YamlGeneratorTest {

    @Test
    void testSimple() {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = Yaml.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("testKey: testValue\n", writer.toString());
    }

    @Test
    void testSequenceOfValues() {
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = Yaml.createGenerator(writer)) {
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

    @Test
    void testMappingOfValues() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try (JsonGenerator generator = Yaml.createGenerator(stream)) {
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

    @Test
    void testExplicitDocumentStart() {
        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_EXPLICIT_START, "true"));
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("---\ntestKey: testValue\n", writer.toString());
    }

    @Test
    void testExplicitDocumentEnd() {
        JsonGeneratorFactory factory = Yaml.createGeneratorFactory(Map.of(Yaml.Settings.DUMP_EXPLICIT_END, "true"));
        StringWriter writer = new StringWriter();

        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.writeStartObject()
                .write("testKey", "testValue")
                .writeEnd();

            writer.flush();
        }

        assertEquals("testKey: testValue\n...\n", writer.toString());
    }

}
