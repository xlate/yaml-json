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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;

final class YamlTestHelper {

    static final String VERSIONS_SOURCE = "io.xlate.yamljson.YamlTestHelper#getTestVersions";

    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    static Set<String> getTestVersions() {
        return Set.of(System.getProperty(Yaml.Settings.YAML_VERSION,
                                         Yaml.Versions.supportedVersions().stream().collect(Collectors.joining(",")))
                            .split(","));
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

    static JsonReader createReader(String version, Reader reader) {
        return Yaml.createReaderFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createReader(reader);
    }

    static JsonReader createReader(String version, InputStream stream) {
        return createReader(version, new InputStreamReader(stream));
    }

    static JsonWriter createWriter(String version, Writer writer) {
        return Yaml.createWriterFactory(Map.of(Yaml.Settings.YAML_VERSION, version)).createWriter(writer);
    }

    static JsonWriter createWriter(String version, OutputStream stream) {
        return createWriter(version, new OutputStreamWriter(stream));
    }
}
