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

import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;

class YamlWriterFactory implements JsonWriterFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final YamlGeneratorFactory generatorFactory;

    YamlWriterFactory() {
        this(Collections.emptyMap());
    }

    YamlWriterFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.generatorFactory = new YamlGeneratorFactory(properties);
    }

    @Override
    public JsonWriter createWriter(Writer writer) {
        Objects.requireNonNull(writer, "writer");
        return new YamlWriter(generatorFactory.createGenerator(writer));
    }

    @Override
    public JsonWriter createWriter(OutputStream out) {
        return createWriter(out, StandardCharsets.UTF_8);
    }

    @Override
    public JsonWriter createWriter(OutputStream out, Charset charset) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(charset, "charset");
        return createWriter(new OutputStreamWriter(out, charset));
    }

    @Override
    public Map<String, ?> getConfigInUse() {
        return Collections.unmodifiableMap(this.properties);
    }

}
