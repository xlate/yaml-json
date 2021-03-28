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

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

class YamlGeneratorFactory implements JsonGeneratorFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final DumpSettings settings;

    YamlGeneratorFactory() {
        this(Collections.emptyMap());
    }

    YamlGeneratorFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.settings = buildDumpSettings(properties);
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        Objects.requireNonNull(writer, "writer");
        return new YamlGenerator(this.settings, writer);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) {
        return createGenerator(out, StandardCharsets.UTF_8);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out, Charset charset) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(charset, "charset");
        return new YamlGenerator(this.settings, new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
