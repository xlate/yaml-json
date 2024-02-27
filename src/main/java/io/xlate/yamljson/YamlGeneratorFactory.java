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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.DumpSettingsBuilder;
import org.yaml.snakeyaml.DumperOptions;

class YamlGeneratorFactory implements JsonGeneratorFactory, SettingsBuilder {

    private static final String SNAKEYAML_ENGINE_PROVIDER = "org.snakeyaml.engine.v2.api.DumpSettings";

    static final Function<Map<String, Object>, Object> SNAKEYAML_FACTORY =
            props -> buildDumperOptions(props);

    static final Function<Map<String, Object>, Object> SNAKEYAML_ENGINE_FACTORY =
            props -> buildDumpSettings(props);

    static void copyBoolean(Map<String, Object> properties, String name, Consumer<Boolean> target) {
        target.accept(getProperty(properties, name, Boolean::valueOf, false));
    }

    @SuppressWarnings("removal")
    static DumperOptions buildDumperOptions(Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(Yaml.Settings.DUMP_CONFIG))
                .map(DumperOptions.class::cast)
                .orElseGet(() -> {
                    DumperOptions options = new DumperOptions();
                    copyBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_START, options::setExplicitStart);
                    copyBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_END, options::setExplicitEnd);
                    return options;
                });
    }

    @SuppressWarnings("removal")
    static DumpSettings buildDumpSettings(Map<String, Object> properties) {
        return Optional.ofNullable(properties.get(Yaml.Settings.DUMP_CONFIG))
                .map(DumpSettings.class::cast)
                .orElseGet(() -> {
                    DumpSettingsBuilder settings = DumpSettings.builder();
                    copyBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_START, settings::setExplicitStart);
                    copyBoolean(properties, Yaml.Settings.DUMP_EXPLICIT_END, settings::setExplicitEnd);
                    return settings.build();
                });
    }

    private final Map<String, Object> properties;
    private final boolean useSnakeYamlEngine;
    private final Object snakeYamlSettings;

    YamlGeneratorFactory(Map<String, ?> properties) {
        this.properties = new HashMap<>(properties);

        Object version = properties.get(Yaml.Settings.YAML_VERSION);

        if (version == null) {
            snakeYamlSettings = Optional.empty()
                .or(() -> loadProvider(this.properties, SNAKEYAML_FACTORY))
                .or(() -> loadProvider(this.properties, SNAKEYAML_ENGINE_FACTORY))
                .orElseThrow(SettingsBuilder::noProvidersFound);

            useSnakeYamlEngine = SNAKEYAML_ENGINE_PROVIDER.equals(snakeYamlSettings.getClass().getName());
        } else {
            useSnakeYamlEngine = Yaml.Versions.V1_2.equals(version);

            if (useSnakeYamlEngine) {
                snakeYamlSettings = loadProvider(this.properties, SNAKEYAML_ENGINE_FACTORY, MOD_SNAKEYAML_ENGINE);
            } else {
                snakeYamlSettings = loadProvider(this.properties, SNAKEYAML_FACTORY, MOD_SNAKEYAML);
            }
        }
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        Objects.requireNonNull(writer, "writer");

        if (useSnakeYamlEngine) {
            var settings = (org.snakeyaml.engine.v2.api.DumpSettings) this.snakeYamlSettings;
            return new SnakeYamlEngineGenerator(settings, writer);
        }

        var settings = (org.yaml.snakeyaml.DumperOptions) this.snakeYamlSettings;
        return new SnakeYamlGenerator(settings, writer);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) {
        return createGenerator(out, StandardCharsets.UTF_8);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out, Charset charset) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(charset, "charset");
        return createGenerator(new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
