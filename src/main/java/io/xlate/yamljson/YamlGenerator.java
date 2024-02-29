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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;

abstract class YamlGenerator<E, S> implements JsonGenerator {

    enum ContextType {
        ARRAY,
        OBJECT
    }

    enum EventType {
        STREAM_START,
        STREAM_END,
        DOCUMENT_START,
        DOCUMENT_END,
        MAPPING_START,
        MAPPING_END,
        SEQUENCE_START,
        SEQUENCE_END
    }

    enum StyleType {
        PLAIN,
        LITERAL,
        FOLDED,
        SINGLE_QUOTED,
        DOUBLE_QUOTED
    }

    interface EventEmitter<E> {
        void emit(E event) throws IOException;
    }

    interface IOOperation {
        void execute() throws IOException;
    }

    static final String VALUE = "value";
    static final String FALSE = "false";
    static final String TRUE = "true";

    protected final Map<String, Object> properties;
    protected final Map<StyleType, S> styleTypes;
    protected final Writer writer;

    private final Deque<ContextType> context = new ArrayDeque<>();
    private final boolean minimizeQuotes;
    private final boolean quoteNumericStrings;
    private final boolean literalBlockStyle;
    private final boolean writePlainBigDecimal;
    private final StringQuotingChecker quoteChecker;

    YamlGenerator(Map<String, Object> properties, Map<StyleType, S> styleTypes, Writer writer) {
        this.properties = properties;
        this.styleTypes = styleTypes;
        this.writer = writer;
        this.minimizeQuotes = parse(properties, Yaml.Settings.DUMP_MINIMIZE_QUOTES, FALSE);
        this.quoteNumericStrings = parse(properties, Yaml.Settings.DUMP_QUOTE_NUMERIC_STRINGS, TRUE);
        this.literalBlockStyle = parse(properties, Yaml.Settings.DUMP_LITERAL_BLOCK_STYLE, FALSE);
        this.writePlainBigDecimal = parse(properties, Yaml.Settings.DUMP_WRITE_PLAIN_BIGDECIMAL, FALSE);
        this.quoteChecker = new StringQuotingChecker(quoteNumericStrings);
    }

    static boolean parse(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties.getOrDefault(key, defaultValue);
        return Boolean.parseBoolean(String.valueOf(value));
    }

    protected abstract E getEvent(EventType type);
    protected abstract void emitEvent(E event) throws IOException;
    protected abstract E buildScalarEvent(String scalarValue, S style);

    void ensureDocumentStarted() {
        if (context.isEmpty()) {
            emit(getEvent(EventType.STREAM_START));
            emit(getEvent(EventType.DOCUMENT_START));
        }
    }

    void assertObjectContext() {
        if (context.isEmpty() || context.peekFirst() != ContextType.OBJECT) {
            throw new JsonGenerationException("Not in object context");
        }
    }

    void emit(E event) {
        try {
            emitEvent(event);
        } catch (IOException e) {
            throw new JsonException("IOException while emitting YAML", e);
        }
    }

    void emitScalar(Object value) {
        emitScalar(value, true, null);
    }

    void emitScalar(String value) {
        emitScalar(value, false, quoteChecker::needToQuoteValue);
    }

    void emitScalar(Object value, boolean forcePlain, Predicate<String> quoteCheck) {
        final String scalarValue;
        final S style;

        if (forcePlain) {
            scalarValue = String.valueOf(value);
            style = styleTypes.get(StyleType.PLAIN);
        } else {
            scalarValue = String.valueOf(value);

            if (minimizeQuotes) {
                if (scalarValue.indexOf('\n') >= 0) {
                    style = styleTypes.get(StyleType.LITERAL);
                } else if (quoteCheck.test(scalarValue)) {
                    // Preserve quotes for keywords, indicators, and numeric strings (if configured)
                    style = styleTypes.get(StyleType.DOUBLE_QUOTED);
                } else {
                    style = styleTypes.get(StyleType.PLAIN);
                }
            } else {
                if (literalBlockStyle && scalarValue.indexOf('\n') >= 0) {
                    style = styleTypes.get(StyleType.LITERAL);
                } else {
                    style = styleTypes.get(StyleType.DOUBLE_QUOTED);
                }
            }
        }

        emit(buildScalarEvent(scalarValue, style));
    }

    protected static void execute(String name, IOOperation operation) {
        try {
            operation.execute();
        } catch (IOException e) {
            throw new JsonException("IOException " + name, e);
        } catch (UncheckedIOException e) {
            throw new JsonException("IOException " + name, e.getCause());
        }
    }

    @Override
    public JsonGenerator writeStartObject() {
        ensureDocumentStarted();
        context.push(ContextType.OBJECT);
        emit(getEvent(EventType.MAPPING_START));
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.OBJECT);
        emit(getEvent(EventType.MAPPING_START));
        return this;
    }

    @Override
    public JsonGenerator writeKey(String name) {
        Objects.requireNonNull(name, "name");
        assertObjectContext();
        emitScalar(name, false, quoteChecker::needToQuoteName);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        ensureDocumentStarted();
        context.push(ContextType.ARRAY);
        emit(getEvent(EventType.SEQUENCE_START));
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.ARRAY);
        emit(getEvent(EventType.SEQUENCE_START));
        return this;
    }

    @Override
    public JsonGenerator write(String name, JsonValue value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, String value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, BigInteger value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, BigDecimal value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, int value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, long value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, double value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator write(String name, boolean value) {
        writeKey(name);
        return write(value);
    }

    @Override
    public JsonGenerator writeNull(String name) {
        writeKey(name);
        return writeNull();
    }

    @Override
    public JsonGenerator writeEnd() {
        if (this.context.isEmpty()) {
            throw new JsonGenerationException("Not in array or object context");
        }

        ContextType contextType = this.context.pop();

        if (contextType == ContextType.OBJECT) {
            emit(getEvent(EventType.MAPPING_END));
        } else {
            emit(getEvent(EventType.SEQUENCE_END));
        }

        if (this.context.isEmpty()) {
            emit(getEvent(EventType.DOCUMENT_END));
            emit(getEvent(EventType.STREAM_END));
        }

        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        Objects.requireNonNull(value, VALUE);

        switch (value.getValueType()) {
        case TRUE:
            emitScalar(Boolean.TRUE);
            break;

        case FALSE:
            emitScalar(Boolean.FALSE);
            break;

        case NUMBER:
            emitScalar(((JsonNumber) value).bigDecimalValue());
            break;

        case STRING:
            emitScalar(((JsonString) value).getString());
            break;

        case ARRAY:
            writeStartArray();
            for (JsonValue entry : value.asJsonArray()) {
                write(entry);
            }
            writeEnd();
            break;

        case OBJECT:
            writeStartObject();
            for (Map.Entry<String, JsonValue> entry : value.asJsonObject().entrySet()) {
                write(entry.getKey(), entry.getValue());
            }
            writeEnd();
            break;

        case NULL:
        default:
            emitScalar((Object) null);
            break;
        }

        return this;
    }

    @Override
    public JsonGenerator write(String value) {
        Objects.requireNonNull(value, VALUE);
        emitScalar(value);
        return this;
    }

    @Override
    public JsonGenerator write(BigDecimal value) {
        Objects.requireNonNull(value, VALUE);
        Object stringValue = writePlainBigDecimal ? value.toPlainString() : value.toString();
        emitScalar(stringValue);
        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        Objects.requireNonNull(value, VALUE);
        emitScalar(value);
        return this;
    }

    @Override
    public JsonGenerator write(int value) {
        emitScalar(value);
        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        emitScalar(value);
        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        if (Double.POSITIVE_INFINITY == value) {
            emitScalar((Object) YamlNumbers.CANONICAL_POSITIVE_INFINITY);
        } else if (Double.NEGATIVE_INFINITY == value) {
            emitScalar((Object) YamlNumbers.CANONICAL_NEGATIVE_INFINITY);
        } else if (Double.isNaN(value)) {
            emitScalar((Object) YamlNumbers.CANONICAL_NAN);
        } else {
            emitScalar(value);
        }

        return this;
    }

    @Override
    public JsonGenerator write(boolean value) {
        emitScalar(value);
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        emitScalar((Object) null);
        return this;
    }

    @Override
    public void close() {
        execute("closing YAML output", () -> {
            flush();
            writer.close();
        });

        if (!context.isEmpty()) {
            throw new JsonGenerationException("Output YAML is incomplete");
        }
    }

    @Override
    public void flush() {
        execute("flushing YAML writer", writer::flush);
    }

}
