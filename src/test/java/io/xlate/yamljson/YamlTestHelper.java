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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

final class YamlTestHelper {

    static final String VERSIONS_SOURCE = "io.xlate.yamljson.YamlTestHelper#getTestVersions";

    private static final SortedSet<String> VERSIONS_PRESENT;

    static {
        SortedSet<String> versions = new TreeSet<>();

        SettingsBuilder.loadProvider(new HashMap<>(), YamlParserFactory.SNAKEYAML_FACTORY)
            .ifPresent(provider -> versions.add(Yaml.Versions.V1_1));
        SettingsBuilder.loadProvider(new HashMap<>(), YamlParserFactory.SNAKEYAML_ENGINE_FACTORY)
            .ifPresent(provider -> versions.add(Yaml.Versions.V1_2));

        VERSIONS_PRESENT = versions;
    }

    static SortedSet<String> detectedVersions() {
        return VERSIONS_PRESENT;
    }

    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    static Set<String> getTestVersions() {
        String testVersions = System.getProperty(Yaml.Settings.YAML_VERSION);

        if (testVersions == null || testVersions.isBlank()) {
            return detectedVersions();
        }

        if ("NONE".equals(testVersions)) {
            return Collections.emptySet();
        }

        return Set.of(testVersions.split(","));
    }

    static void testEachVersion(ThrowingConsumer<String> testCase) {
        for (String version : getTestVersions()) {
            try {
                testCase.accept(version);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static boolean isOnlySupportedVersion(String version) {
        return detectedVersions().size() == 1 && detectedVersions().contains(version);
    }

    //////////

    static JsonParser createParser(String version, Reader reader) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createParser(reader);
        }

        return Yaml.createParserFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createParser(reader);
    }

    static JsonParser createParser(String version, InputStream stream) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createParser(stream);
        }

        return createParser(stream, Map.of(Yaml.Settings.YAML_VERSION, version));
    }

    static JsonParser createParser(InputStream stream, Map<String, ?> properties) {
        return Yaml.createParserFactory(properties).createParser(stream);
    }

    static void readFully(JsonParser parser) {
        while (parser.hasNext()) {
            parser.next();
        }
    }

    //////////

    static JsonReader createReader(String version, Reader reader) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createReader(reader);
        }

        return Yaml.createReaderFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createReader(reader);
    }

    static JsonReader createReader(String version, InputStream stream) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createReader(stream);
        }

        return createReader(stream, Map.of(Yaml.Settings.YAML_VERSION, version));
    }

    static JsonReader createReader(InputStream stream, Map<String, ?> properties) {
        return Yaml.createReaderFactory(properties).createReader(stream);
    }

    //////////

    static JsonGenerator createGenerator(String version, Writer writer) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createGenerator(writer);
        }

        return Yaml.createGeneratorFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createGenerator(writer);
    }

    static JsonGenerator createGenerator(String version, OutputStream stream) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createGenerator(stream);
        }

        return Yaml.createGeneratorFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createGenerator(stream);
    }

    //////////

    static JsonWriter createWriter(String version, Writer writer) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createWriter(writer);
        }

        return Yaml.createWriterFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createWriter(writer);
    }

    static JsonWriter createWriter(String version, OutputStream stream) {
        if (isOnlySupportedVersion(version)) {
            return Yaml.createWriter(stream);
        }

        return Yaml.createWriterFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createWriter(stream);
    }
}
