package io.xlate.yamljson;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;

class YamlWriterTest {

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void testWriteObject() throws IOException {
        assertDoesNotThrow(() -> {
            StringWriter sink = new StringWriter();

            try (InputStream source = getClass().getResourceAsStream("/simpleapi.yaml");
                 JsonReader reader = Yaml.createReader(source);
                 JsonWriter writer = Yaml.createWriter(sink)) {

                JsonValue value = reader.readValue();
                writer.write(value);
            }

            System.out.print(sink.toString());
        });
    }

    @Test
    void testScalarStyles() throws IOException {
        StringWriter sink = new StringWriter();

        try (JsonWriter writer = Yaml.createWriter(sink)) {
            JsonValue value = Json.createArrayBuilder().add("#http://example.com").build();
            writer.write(value);
        }

        assertEquals("- '#http://example.com'\n", sink.toString());
    }
}
