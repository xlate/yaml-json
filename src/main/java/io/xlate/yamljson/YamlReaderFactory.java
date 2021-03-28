package io.xlate.yamljson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.snakeyaml.engine.v2.api.lowlevel.Parse;

import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;

class YamlReaderFactory implements JsonReaderFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final Parse parse;

    YamlReaderFactory() {
        this(Collections.emptyMap());
    }

    YamlReaderFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.parse = new Parse(buildLoadSettings(properties));
    }

    private YamlReader createYamlReader(Reader reader) {
        return new YamlReader(parse, reader);
    }

    @Override
    public JsonReader createReader(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return createYamlReader(reader);
    }

    @Override
    public JsonReader createReader(InputStream in) {
        Objects.requireNonNull(in, "in");
        return createYamlReader(new InputStreamReader(in));
    }

    @Override
    public JsonReader createReader(InputStream in, Charset charset) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(charset, "charset");
        return createReader(new InputStreamReader(in, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
