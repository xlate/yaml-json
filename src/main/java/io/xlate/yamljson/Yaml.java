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

import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
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
 * The following example shows how to create a parser to parse simple key/value
 * pair:
 *
 * <pre>
 * <code>
 * StringReader reader = new StringReader("---\nkey: value\n");
 * JsonParser parser = Yaml.createParser(reader);
 * </code>
 * </pre>
 *
 * <p>
 * All the methods in this class are safe for use by multiple concurrent
 * threads.
 *
 * @see jakarta.json.Json
 */
public final class Yaml {

    /**
     * Constants of supported YAML versions
     */
    public static final class Versions {
        /**
         * Use snakeyaml to parse or generate YAML v1.1
         */
        public static final String V1_1 = "v1.1";

        /**
         * Use snakeyaml-engine to parse or generate YAML v1.2
         */
        public static final String V1_2 = "v1.2";

        private Versions() {
        }
    }

    /**
     * Constants of supported property keys.
     */
    public static final class Settings {

        private static final String PRE = "io.xlate.yamljson.";

        private Settings() {
        }

        /**
         * Explicitly set the YAML version used for parsing or generating YAML.
         * This will determine which underlying YAML library to use.
         * <p>
         * This configuration setting may be omitted if only one YAML library is
         * present on the class/module path.
         *
         * <ul>
         * <li>{@link Versions#V1_1 V1_1} requires snakeyaml to be present on
         * the class/module path
         * <li>{@link Versions#V1_2 V1_2} requires snakeyaml-engine to be
         * present on the class/module path
         * </ul>
         *
         * @see Versions#V1_1
         * @see Versions#V1_2
         */
        public static final String YAML_VERSION = PRE + "YAML_VERSION";

        /**
         * The maximum number of scalars to which an alias (or chain of aliases via arrays/objects)
         * may expand.
         *
         * @since 0.1.0
         */
        public static final String LOAD_MAX_ALIAS_EXPANSION_SIZE = "LOAD_MAX_ALIAS_EXPANSION_SIZE";

        /**
         * Requires snakeyaml-engine, not supported with snakeyaml.
         *
         * @see org.snakeyaml.engine.v2.api.LoadSettingsBuilder#setUseMarks(boolean)
         *
         * @since 0.1.0
         * @deprecated use
         *             {@link org.snakeyaml.engine.v2.api.LoadSettingsBuilder#setUseMarks(boolean)
         *             LoadSettingsBuilder#setUseMarks} with
         *             {@link #LOAD_CONFIG} to configure this option.
         */
        @Deprecated(since = "0.2", forRemoval = true)
        public static final String LOAD_USE_MARKS = "LOAD_USE_MARKS"; // NOSONAR

        /**
         * Set to true if the document start must be explicitly indicated by
         * adding {@code ---} at the beginning of the document.
         *
         * @see org.yaml.snakeyaml.DumperOptions#setExplicitStart(boolean)
         * @see org.snakeyaml.engine.v2.api.DumpSettingsBuilder#setExplicitStart(boolean)
         *
         * @deprecated use
         *             {@link org.yaml.snakeyaml.DumperOptions#setExplicitStart(boolean)
         *             DumperOptions#setExplicitStart} or
         *             {@link org.snakeyaml.engine.v2.api.DumpSettingsBuilder#setExplicitStart(boolean)
         *             DumpSettingsBuilder#setExplicitStart} with
         *             {@link #DUMP_CONFIG} to configure this option.
         */
        @Deprecated(since = "0.2", forRemoval = true)
        public static final String DUMP_EXPLICIT_START = "DUMP_EXPLICIT_START"; // NOSONAR

        /**
         * Set to true if the document end must be explicitly indicated by
         * adding {@code ...} at the end of the document.
         *
         * @see org.yaml.snakeyaml.DumperOptions#setExplicitEnd(boolean)
         * @see org.snakeyaml.engine.v2.api.DumpSettingsBuilder#setExplicitEnd(boolean)
         *
         * @deprecated use
         *             {@link org.yaml.snakeyaml.DumperOptions#setExplicitEnd(boolean)
         *             DumperOptions#setExplicitEnd} or
         *             {@link org.snakeyaml.engine.v2.api.DumpSettingsBuilder#setExplicitEnd(boolean)
         *             DumpSettingsBuilder#setExplicitEnd} with
         *             {@link #DUMP_CONFIG} to configure this option.
         */
        @Deprecated(since = "0.2", forRemoval = true)
        public static final String DUMP_EXPLICIT_END = "DUMP_EXPLICIT_END"; // NOSONAR

        /**
         * Used to pass a pre-configured
         * {@linkplain org.yaml.snakeyaml.LoaderOptions LoaderOptions} or
         * {@linkplain org.snakeyaml.engine.v2.api.LoadSettings LoadSettings}
         * instance for use when reading or parsing YAML.
         *
         * @since 0.2
         */
        public static final String LOAD_CONFIG = PRE + "LOAD_CONFIG";

        /**
         * Used to pass a pre-configured
         * {@linkplain org.yaml.snakeyaml.DumperOptions DumperOptions} or
         * {@linkplain org.snakeyaml.engine.v2.api.DumpSettings DumpSettings}
         * instance for use when writing or generating YAML.
         *
         * @since 0.2
         */
        public static final String DUMP_CONFIG = PRE + "DUMP_CONFIG";

        /**
         * Whether strings will be rendered without quotes (true) or with quotes
         * (false, default).
         * <p>
         * Minimized quote usage makes for more human readable output; however,
         * content is limited to printable characters according to the rules of
         * <a href=
         * "http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal
         * block style</a>.
         *
         * @since 0.2
         */
        public static final String DUMP_MINIMIZE_QUOTES = PRE + "DUMP_MINIMIZE_QUOTES";

        /**
         * Whether numeric values stored as strings will be rendered with quotes
         * (true, default) or without quotes (false) when
         * {@link #DUMP_MINIMIZE_QUOTES} is enabled.
         *
         * @since 0.2
         */
        public static final String DUMP_QUOTE_NUMERIC_STRINGS = PRE + "DUMP_QUOTE_NUMERIC_STRINGS";

        /**
         * Whether strings containing newlines should use <a href=
         * "http://www.yaml.org/spec/1.2/spec.html#style/block/literal">literal
         * block style</a>. This automatically enabled when
         * {@link #DUMP_MINIMIZE_QUOTES} is set.
         *
         * @since 0.2
         */
        public static final String DUMP_LITERAL_BLOCK_STYLE = PRE + "DUMP_LITERAL_BLOCK_STYLE";

        /**
         * Feature that determines whether {@link java.math.BigDecimal} entries are
         * serialized using {@link java.math.BigDecimal#toPlainString()} to prevent
         * values to be written using scientific notation.
         *<p>
         * Feature is disabled by default, so default output mode is used; this generally
         * depends on how {@link java.math.BigDecimal} has been created.
         *
         * @since 0.2
         */
        public static final String DUMP_WRITE_PLAIN_BIGDECIMAL = PRE + "DUMP_WRITE_PLAIN_BIGDECIMAL";
    }

    private static final YamlProvider PROVIDER = new YamlProvider();

    private Yaml() {
    }

    /**
     * Obtain the underlying YAML provider used by the other methods of this
     * class. This may be useful in cases where a JsonProvider is needed.
     * <p>
     * Repeat calls to this method will return the same instance.
     *
     * @return a YAML provider implementation of the JsonProvider contract
     */
    public static JsonProvider provider() {
        return PROVIDER;
    }

    /**
     * Creates a YAML parser from a character stream.
     *
     * @param reader
     *            i/o reader from which JSON is to be read
     * @return a YAML parser
     *
     * @see jakarta.json.Json#createParser(Reader)
     *
     */
    public static JsonParser createParser(Reader reader) {
        return provider().createParser(reader);
    }

    /**
     * Creates a YAML parser from a byte stream. The character encoding of the
     * stream is determined as specified in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in
     *            i/o stream from which YAML is to be read
     * @throws JsonException
     *             if encoding cannot be determined or i/o error (IOException
     *             would be cause of JsonException)
     * @return a YAML parser
     *
     * @see jakarta.json.Json#createParser(InputStream)
     */
    public static JsonParser createParser(InputStream in) {
        return provider().createParser(in);
    }

    /**
     * Creates a YAML generator for writing YAML to a character stream.
     *
     * @param writer
     *            a i/o writer to which YAML is written
     * @return a YAML generator
     *
     * @see jakarta.json.Json#createGenerator(Writer)
     */
    public static JsonGenerator createGenerator(Writer writer) {
        return provider().createGenerator(writer);
    }

    /**
     * Creates a YAML generator for writing YAML to a byte stream.
     *
     * @param out
     *            i/o stream to which YAML is written
     * @return a YAML generator
     *
     * @see jakarta.json.Json#createGenerator(OutputStream)
     */
    public static JsonGenerator createGenerator(OutputStream out) {
        return provider().createGenerator(out);
    }

    /**
     * Creates a parser factory for creating {@link JsonParser} objects (for
     * parsing YAML). The factory is configured with the specified map of
     * provider specific configuration properties. Provider implementations
     * should ignore any unsupported configuration properties specified in the
     * map.
     *
     * @param config
     *            a map of provider specific properties to configure the YAML
     *            parsers. The map may be empty or null
     * @return YAML parser factory
     *
     * @see jakarta.json.Json#createParserFactory(Map)
     */
    public static JsonParserFactory createParserFactory(Map<String, ?> config) {
        return provider().createParserFactory(config);
    }

    /**
     * Creates a generator factory for creating {@link JsonGenerator} objects
     * (for generating YAML). The factory is configured with the specified map
     * of provider specific configuration properties. Provider implementations
     * should ignore any unsupported configuration properties specified in the
     * map.
     *
     * @param config
     *            a map of provider specific properties to configure the YAML
     *            generators. The map may be empty or null
     * @return YAML generator factory
     *
     * @see jakarta.json.Json#createGeneratorFactory(Map)
     */
    public static JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config) {
        return provider().createGeneratorFactory(config);
    }

    /**
     * Creates a YAML writer to write a JSON {@link JsonObject object} or
     * {@link JsonArray array} structure to the specified character stream as
     * YAML.
     *
     * @param writer
     *            to which JSON object or array is written
     * @return a YAML writer
     *
     * @see jakarta.json.Json#createWriter(Writer)
     */
    public static JsonWriter createWriter(Writer writer) {
        return provider().createWriter(writer);
    }

    /**
     * Creates a YAML writer to write a JSON {@link JsonObject object} or
     * {@link JsonArray array} structure to the specified byte stream as YAML.
     * Characters written to the stream are encoded into bytes using UTF-8
     * encoding.
     *
     * @param out
     *            to which JSON object or array is written
     * @return a YAML writer
     *
     * @see jakarta.json.Json#createWriter(OutputStream)
     */
    public static JsonWriter createWriter(OutputStream out) {
        return provider().createWriter(out);
    }

    /**
     * Creates a YAML reader from a character stream.
     *
     * @param reader
     *            a reader from which YAML is to be read
     * @return a YAML reader
     *
     * @see jakarta.json.Json#createReader(Reader)
     */
    public static JsonReader createReader(Reader reader) {
        return provider().createReader(reader);
    }

    /**
     * Creates a YAML reader from a byte stream. The character encoding of the
     * stream is determined as described in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in
     *            a byte stream from which YAML is to be read
     * @return a YAML reader
     *
     * @see jakarta.json.Json#createReader(InputStream)
     */
    public static JsonReader createReader(InputStream in) {
        return provider().createReader(in);
    }

    /**
     * Creates a reader factory for creating {@link JsonReader} objects (for
     * reading YAML). The factory is configured with the specified map of
     * provider specific configuration properties. Provider implementations
     * should ignore any unsupported configuration properties specified in the
     * map.
     *
     * @param config
     *            a map of provider specific properties to configure the YAML
     *            readers. The map may be empty or null
     * @return a YAML reader factory
     *
     * @see jakarta.json.Json#createReaderFactory(Map)
     */
    public static JsonReaderFactory createReaderFactory(Map<String, ?> config) {
        return provider().createReaderFactory(config);
    }

    /**
     * Creates a writer factory for creating {@link JsonWriter} objects (for
     * writing YAML). The factory is configured with the specified map of
     * provider specific configuration properties. Provider implementations
     * should ignore any unsupported configuration properties specified in the
     * map.
     *
     * @param config
     *            a map of provider specific properties to configure the YAML
     *            writers. The map may be empty or null
     * @return a YAML writer factory
     *
     * @see jakarta.json.Json#createWriterFactory(Map)
     */
    public static JsonWriterFactory createWriterFactory(Map<String, ?> config) {
        return provider().createWriterFactory(config);
    }

}
