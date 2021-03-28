package io.xlate.yamljson;

import java.io.Writer;

import org.snakeyaml.engine.v2.api.DumpSettings;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;

class YamlWriter implements JsonWriter {

    final YamlGenerator generator;
    boolean writable = true;

    YamlWriter(DumpSettings settings, Writer writer) {
        this.generator = new YamlGenerator(settings, writer);
    }

    @Override
    public void writeArray(JsonArray array) {
        write((JsonValue) array);
    }

    @Override
    public void writeObject(JsonObject object) {
        write((JsonValue) object);
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
