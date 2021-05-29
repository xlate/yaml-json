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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

class YamlGeneratorFactory implements JsonGeneratorFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final boolean useSnakeYamlEngine;
    private final org.yaml.snakeyaml.DumperOptions snakeYamlOptions;
    private final org.snakeyaml.engine.v2.api.DumpSettings snakeYamlSettings;

    YamlGeneratorFactory() {
        this(Collections.emptyMap());
    }

    YamlGeneratorFactory(Map<String, ?> properties) {
        this.properties = properties;
        Object version = properties.get(Yaml.Settings.YAML_VERSION);
        this.useSnakeYamlEngine = Yaml.Settings.YAML_VERSION_1_2.equals(version);

        if (useSnakeYamlEngine) {
            this.snakeYamlOptions = null;
            this.snakeYamlSettings = buildDumpSettings(properties);
        } else {
            this.snakeYamlOptions = buildDumperOptions(properties);
            this.snakeYamlSettings = null;
        }
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        Objects.requireNonNull(writer, "writer");
        if (useSnakeYamlEngine) {
            return new YamlGenerator(this.snakeYamlSettings, writer);
        }
        return new YamlGenerator1_1(this.snakeYamlOptions, writer);
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
