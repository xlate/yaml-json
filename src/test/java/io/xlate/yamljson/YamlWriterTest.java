package io.xlate.yamljson;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
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

                JsonObject value = reader.readObject();
                writer.writeObject(value);
                assertThrows(IllegalStateException.class, () -> writer.write(value));
            }

            System.out.print(sink.toString());
        });
    }

    @Test
    void testScalarStyles() throws IOException {
        StringWriter sink = new StringWriter();

        try (JsonWriter writer = Yaml.createWriter(sink)) {
            JsonArray value = Json.createArrayBuilder().add("#http://example.com").build();
            writer.writeArray(value);
            assertThrows(IllegalStateException.class, () -> writer.write(value));
        }

        assertEquals("- '#http://example.com'\n", sink.toString());
    }
}
