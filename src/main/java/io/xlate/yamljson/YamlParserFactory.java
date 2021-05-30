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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

class YamlParserFactory implements JsonParserFactory, SettingsBuilder {

    private static final String MOD_SNAKEYAML = "org.yaml.snakeyaml";
    private static final String MOD_SNAKEYAML_ENGINE = "org.snakeyaml.engine";
    private static final String MISSING_MODULE_MESSAGE = "Required module not found: %s. "
            + "Ensure module is present on module path. Add to application module-info or "
            + "include with --add-modules command line option.";

    private final Map<String, ?> properties;
    private final boolean useSnakeYamlEngine;
    private final Object snakeYamlProvider;

    YamlParserFactory(Map<String, ?> properties) {
        this.properties = properties;

        Object version = properties.get(Yaml.Settings.YAML_VERSION);
        useSnakeYamlEngine = Yaml.Settings.YAML_VERSION_1_2.equals(version);

        if (useSnakeYamlEngine) {
            snakeYamlProvider = loadProvider(() -> new org.snakeyaml.engine.v2.api.lowlevel.Parse(buildLoadSettings(properties)),
                                             MOD_SNAKEYAML_ENGINE);
        } else {
            snakeYamlProvider = loadProvider(() -> new org.yaml.snakeyaml.Yaml(buildLoaderOptions(properties)),
                                             MOD_SNAKEYAML);
        }
    }

    Object loadProvider(Supplier<Object> providerSupplier, String providerModule) {
        try {
            return providerSupplier.get();
        } catch (Exception | NoClassDefFoundError e) {
            throw new IllegalStateException(String.format(MISSING_MODULE_MESSAGE, providerModule), e);
        }
    }

    AbstractYamlParser<?, ?> createYamlParser(Reader reader) {
        if (useSnakeYamlEngine) {
            var provider = (org.snakeyaml.engine.v2.api.lowlevel.Parse) snakeYamlProvider;
            return new YamlParser(provider.parseReader(reader).iterator(), reader);
        }

        var provider = (org.yaml.snakeyaml.Yaml) snakeYamlProvider;
        return new YamlParser1_1(provider.parse(reader).iterator(), reader);
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
