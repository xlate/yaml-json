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

import static io.xlate.yamljson.SettingsBuilder.getProperty;
import static io.xlate.yamljson.SettingsBuilder.loadProvider;
import static io.xlate.yamljson.SettingsBuilder.replace;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.yaml.snakeyaml.LoaderOptions;

class YamlParserFactory implements JsonParserFactory, SettingsBuilder {

    private static final String SNAKEYAML_ENGINE_PROVIDER = "org.snakeyaml.engine.v2.api.lowlevel.Parse";

    static final Function<Map<String, Object>, Object> SNAKEYAML_FACTORY =
            // No load properties supported currently
            props -> new org.yaml.snakeyaml.Yaml(buildLoaderOptions(props));

    static final Function<Map<String, Object>, Object> SNAKEYAML_ENGINE_FACTORY =
            props -> new org.snakeyaml.engine.v2.api.lowlevel.Parse(buildLoadSettings(props));

    static LoaderOptions buildLoaderOptions(Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(Yaml.Settings.LOAD_CONFIG))
                .map(LoaderOptions.class::cast)
                .orElseGet(LoaderOptions::new);
    }

    @SuppressWarnings("removal")
    static LoadSettings buildLoadSettings(Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(Yaml.Settings.LOAD_CONFIG))
                .map(LoadSettings.class::cast)
                .orElseGet(() -> LoadSettings.builder()
                        .setUseMarks(getProperty(properties, Yaml.Settings.LOAD_USE_MARKS, Boolean::valueOf, true))
                        .build());
    }

    private final Map<String, Object> properties;
    private final boolean useSnakeYamlEngine;
    private final Object snakeYamlProvider;
    private final Function<InputStream, Reader> yamlReaderProvider;

    YamlParserFactory(Map<String, ?> properties) {
        this.properties = new HashMap<>(properties);

        Object version = properties.get(Yaml.Settings.YAML_VERSION);

        if (version == null) {
            snakeYamlProvider = Optional.empty()
                .or(() -> loadProvider(this.properties, SNAKEYAML_FACTORY))
                .or(() -> loadProvider(this.properties, SNAKEYAML_ENGINE_FACTORY))
                .orElseThrow(SettingsBuilder::noProvidersFound);

            useSnakeYamlEngine = SNAKEYAML_ENGINE_PROVIDER.equals(snakeYamlProvider.getClass().getName());
        } else {
            useSnakeYamlEngine = Yaml.Versions.V1_2.equals(version);

            if (useSnakeYamlEngine) {
                snakeYamlProvider = loadProvider(this.properties, SNAKEYAML_ENGINE_FACTORY, MOD_SNAKEYAML_ENGINE);
            } else {
                snakeYamlProvider = loadProvider(this.properties, SNAKEYAML_FACTORY, MOD_SNAKEYAML);
            }
        }

        yamlReaderProvider = useSnakeYamlEngine
                ? org.snakeyaml.engine.v2.api.YamlUnicodeReader::new
                : org.yaml.snakeyaml.reader.UnicodeReader::new;

        // Ensure this property is always set, defaulting to Long.MAX_VALUE
        replace(this.properties, Yaml.Settings.LOAD_MAX_ALIAS_EXPANSION_SIZE, Long::valueOf, Long.MAX_VALUE);
    }

    YamlParser<?, ?> createYamlParser(InputStream stream) { // NOSONAR - ignore use of wildcards
        return createYamlParser(yamlReaderProvider.apply(stream));
    }

    YamlParser<?, ?> createYamlParser(Reader reader) { // NOSONAR - ignore use of wildcards
        if (useSnakeYamlEngine) {
            var provider = (org.snakeyaml.engine.v2.api.lowlevel.Parse) snakeYamlProvider;
            return new SnakeYamlEngineParser(provider.parseReader(reader).iterator(), reader, properties);
        }

        var provider = (org.yaml.snakeyaml.Yaml) snakeYamlProvider;
        return new SnakeYamlParser(provider.parse(reader).iterator(), reader, properties);
    }

    @Override
    public JsonParser createParser(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return createYamlParser(reader);
    }

    @Override
    public JsonParser createParser(InputStream in) {
        return createYamlParser(in);
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
