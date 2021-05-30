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
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;

class YamlReaderFactory implements JsonReaderFactory, SettingsBuilder {

    private final Map<String, ?> properties;
    private final YamlParserFactory parserFactory;

    YamlReaderFactory(Map<String, ?> properties) {
        this.properties = properties;
        this.parserFactory = new YamlParserFactory(properties);
    }

    private YamlReader createYamlReader(Reader reader) {
        return new YamlReader(parserFactory.createYamlParser(reader));
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
