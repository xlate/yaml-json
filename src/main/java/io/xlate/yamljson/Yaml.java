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
 * Factory class for creating processing objects for YAML using the Jakarta JSON
 * API.
 * <p>
 * This class provides the most commonly used methods for creating these objects
 * and their corresponding factories. The factory classes provide all the
 * various ways to create these objects.
 *
 * <p>
 * The methods in this class utilize a singleton provider instance using
 * specifically for support of YAML. This class uses the provider instance to
 * create JSON processing objects.
 *
 * <p>
 * The following example shows how to create a parser to parse an empty array:
 *
 * <pre>
 * <code>
 * StringReader reader = new StringReader("[]");
 * JsonParser parser = Json.createParser(reader);
 * </code>
 * </pre>
 *
 * <p>
 * All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public final class Yaml {

    private static final JsonProvider PROVIDER = new YamlProvider();

    private Yaml() {
    }

    private static JsonProvider provider() {
        return PROVIDER;
    }

    /**
     * @see jakarta.json.Json#createParser(Reader)
     */
    public static JsonParser createParser(Reader reader) {
        return provider().createParser(reader);
    }

    /**
     * @see jakarta.json.Json#createParser(InputStream)
     */
    public static JsonParser createParser(InputStream in) {
        return provider().createParser(in);
    }

    /**
     * @see jakarta.json.Json#createGenerator(Writer)
     */
    public static JsonGenerator createGenerator(Writer writer) {
        return provider().createGenerator(writer);
    }

    /**
     * @see jakarta.json.Json#createGenerator(OutputStream)
     */
    public static JsonGenerator createGenerator(OutputStream out) {
        return provider().createGenerator(out);
    }

    /**
     * @see jakarta.json.Json#createParserFactory(Map)
     */
    public static JsonParserFactory createParserFactory(Map<String, ?> config) {
        return provider().createParserFactory(config);
    }

    /**
     * @see jakarta.json.Json#createGeneratorFactory(Map)
     */
    public static JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config) {
        return provider().createGeneratorFactory(config);
    }

    /**
     * @see jakarta.json.Json#createWriter(Writer)
     */
    public static JsonWriter createWriter(Writer writer) {
        return provider().createWriter(writer);
    }

    /**
     * @see jakarta.json.Json#createWriter(OutputStream)
     */
    public static JsonWriter createWriter(OutputStream out) {
        return provider().createWriter(out);
    }

    /**
     * @see jakarta.json.Json#createReader(Reader)
     */
    public static JsonReader createReader(Reader reader) {
        return provider().createReader(reader);
    }

    /**
     * @see jakarta.json.Json#createReader(InputStream)
     */
    public static JsonReader createReader(InputStream in) {
        return provider().createReader(in);
    }

    /**
     * @see jakarta.json.Json#createReaderFactory(Map)
     */
    public static JsonReaderFactory createReaderFactory(Map<String, ?> config) {
        return provider().createReaderFactory(config);
    }

    /**
     * @see jakarta.json.Json#createWriterFactory(Map)
     */
    public static JsonWriterFactory createWriterFactory(Map<String, ?> config) {
        return provider().createWriterFactory(config);
    }

}
