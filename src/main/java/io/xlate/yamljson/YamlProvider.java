/*
 * Copyright 2021 xlate.io LLC, http://www.xlate.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.xlate.yamljson;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

/**
 * Extension of {@link JsonProvider} providing access to YAML processing
 * objects. This provider will NOT be made available via the service loader
 * mechanism to avoid conflicts with other JSON providers available.
 */
final class YamlProvider extends JsonProvider {

    private final YamlParserFactory defaultParserFactory;
    private final YamlReaderFactory defaultReaderFactory;
    private final YamlGeneratorFactory defaultGeneratorFactory;
    private final YamlWriterFactory defaultWriterFactory;
    final String defaultVersion;

    public YamlProvider() {
        Set<String> supportedVersions = Yaml.Versions.supportedVersions();

        if (supportedVersions.isEmpty()) {
            throw new IllegalStateException("No YAML providers found on class/module path!");
        } else {
            // v1.1 if available, otherwise v1.2
            defaultVersion = supportedVersions.iterator().next();
        }

        var defaultProperties = Map.of(Yaml.Settings.YAML_VERSION, defaultVersion);

        defaultParserFactory = new YamlParserFactory(defaultProperties);
        defaultReaderFactory = new YamlReaderFactory(defaultProperties, defaultParserFactory);
        defaultGeneratorFactory = new YamlGeneratorFactory(defaultProperties);
        defaultWriterFactory = new YamlWriterFactory(defaultProperties, defaultGeneratorFactory);
    }

    @Override
    public JsonParser createParser(Reader reader) {
        return defaultParserFactory.createParser(reader);
    }

    @Override
    public JsonParser createParser(InputStream in) {
        return defaultParserFactory.createParser(in);
    }

    @Override
    public JsonParserFactory createParserFactory(Map<String, ?> config) {
        return new YamlParserFactory(config);
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        return defaultGeneratorFactory.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) {
        return defaultGeneratorFactory.createGenerator(out);
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config) {
        return new YamlGeneratorFactory(config);
    }

    @Override
    public JsonReader createReader(Reader reader) {
        return defaultReaderFactory.createReader(reader);
    }

    @Override
    public JsonReader createReader(InputStream in) {
        return defaultReaderFactory.createReader(in);
    }

    @Override
    public JsonWriter createWriter(Writer writer) {
        return defaultWriterFactory.createWriter(writer);
    }

    @Override
    public JsonWriter createWriter(OutputStream out) {
        return defaultWriterFactory.createWriter(out);
    }

    @Override
    public JsonWriterFactory createWriterFactory(Map<String, ?> config) {
        return new YamlWriterFactory(config);
    }

    @Override
    public JsonReaderFactory createReaderFactory(Map<String, ?> config) {
        return new YamlReaderFactory(config);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        throw new UnsupportedOperationException("createObjectBuilder()");
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        throw new UnsupportedOperationException("createArrayBuilder()");
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(Map<String, ?> config) {
        throw new UnsupportedOperationException("createBuilderFactory(Map)");
    }

}
