package io.xlate.yamljson;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.snakeyaml.engine.v2.api.DumpSettings;

import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;

class YamlWriterFactory implements JsonWriterFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final DumpSettings settings;

    YamlWriterFactory() {
        this(Collections.emptyMap());
    }

    YamlWriterFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.settings = buildDumpSettings(properties);
    }

    @Override
    public JsonWriter createWriter(Writer writer) {
        Objects.requireNonNull(writer, "writer");
        return new YamlWriter(this.settings, writer);
    }

    @Override
    public JsonWriter createWriter(OutputStream out) {
        return createWriter(out, StandardCharsets.UTF_8);
    }

    @Override
    public JsonWriter createWriter(OutputStream out, Charset charset) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(charset, "charset");
        return new YamlWriter(this.settings, new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
