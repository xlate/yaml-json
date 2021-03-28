package io.xlate.yamljson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.snakeyaml.engine.v2.api.lowlevel.Parse;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

class YamlParserFactory implements JsonParserFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final Parse parse;

    YamlParserFactory() {
        this(Collections.emptyMap());
    }

    YamlParserFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.parse = new Parse(buildLoadSettings(properties));
    }

    private YamlParser createYamlParser(Reader reader) {
        return new YamlParser(parse, reader);
    }

    @Override
    public JsonParser createParser(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return createYamlParser(reader);
    }

    @Override
    public JsonParser createParser(InputStream in) {
        return createParser(in, StandardCharsets.UTF_8);
    }

    @Override
    public JsonParser createParser(InputStream in, Charset charset) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(charset, "charset");
        return createYamlParser(new InputStreamReader(in, charset));
    }

    @Override
    public JsonParser createParser(JsonObject obj) {
        throw new UnsupportedOperationException("createParser(JsonObject)");
    }

    @Override
    public JsonParser createParser(JsonArray array) {
        throw new UnsupportedOperationException("createParser(JsonArray)");
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
