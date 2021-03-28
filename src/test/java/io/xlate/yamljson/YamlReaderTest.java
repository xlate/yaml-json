package io.xlate.yamljson;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    void assertUnreadable(String inputValue, Consumer<JsonReader> reader) throws IOException {
        try (Reader s = new StringReader(inputValue); JsonReader r = Yaml.createReader(s)) {
            assertThrows(JsonParsingException.class, () -> reader.accept(r));
        }
    }

    JsonValue assertReadable(String inputValue, Function<JsonReader, JsonValue> reader) throws IOException {
        try (Reader s = new StringReader(inputValue); JsonReader r = Yaml.createReader(s)) {
            return reader.apply(r);
        }
    }

    @Test
    void testReadObject() throws IOException {
        JsonObject object = null;

        try (InputStream source = getClass().getResourceAsStream("/simpleapi.yaml");
                JsonReader reader = Yaml.createReader(source)) {
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
        inputValue = String.format(inputValue);
        JsonValue value = null;

        switch (readType) {
        case STRUCTURE:
            value = assertReadable(inputValue, JsonReader::read);
            break;

        case OBJECT:
            assertUnreadable(inputValue, JsonReader::readArray);

            assertReadable(inputValue, JsonReader::read);
            value = assertReadable(inputValue, JsonReader::readObject);
            break;

        case ARRAY:
            assertUnreadable(inputValue, JsonReader::readObject);

            assertReadable(inputValue, JsonReader::read);
            value = assertReadable(inputValue, JsonReader::readArray);
            break;

        case SCALAR:
            assertUnreadable(inputValue, JsonReader::read);
            assertUnreadable(inputValue, JsonReader::readArray);
            assertUnreadable(inputValue, JsonReader::readObject);

            value = assertReadable(inputValue, JsonReader::readValue);
            break;
        }

        assertTrue(expectedType.isAssignableFrom(value.getClass()));
    }
}
