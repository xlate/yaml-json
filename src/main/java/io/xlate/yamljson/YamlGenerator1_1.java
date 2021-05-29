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
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ImplicitTuple;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.events.StreamEndEvent;
import org.yaml.snakeyaml.events.StreamStartEvent;

import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;

class YamlGenerator1_1 implements JsonGenerator {

    static final String VALUE = "value";

    static final ImplicitTuple omitTags = new ImplicitTuple(true, true);

    static final Event STREAM_START = new StreamStartEvent(null, null);
    static final Event STREAM_END = new StreamEndEvent(null, null);

    static final Event DOCUMENT_START_DEFAULT = new DocumentStartEvent(null, null, false, null, Collections.emptyMap());
    static final Event DOCUMENT_START_EXPLICIT = new DocumentStartEvent(null, null, true, null, Collections.emptyMap());

    static final Event DOCUMENT_END_DEFAULT = new DocumentEndEvent(null, null, false);
    static final Event DOCUMENT_END_EXPLICIT = new DocumentEndEvent(null, null, true);

    static final Event MAPPING_START = new MappingStartEvent(null, null, true, null, null, FlowStyle.AUTO);
    static final Event MAPPING_END = new MappingEndEvent(null, null);

    static final Event SEQUENCE_START = new SequenceStartEvent(null, null, true, null, null, FlowStyle.AUTO);
    static final Event SEQUENCE_END = new SequenceEndEvent(null, null);

    static final StringQuotingChecker quoteChecker = new StringQuotingChecker();

    final Writer writer;
    final DumperOptions settings;
    final Emitter emitter;
    final Deque<ContextType> context = new ArrayDeque<>();

    YamlGenerator1_1(DumperOptions settings, Writer writer) {
        this.writer = writer;
        this.settings = settings;
        this.emitter = new Emitter(writer, settings);
    }

    void emit(Event event) {
        try {
            emitter.emit(event);
        } catch (IOException e) {
            // TODO: exception message
            throw new JsonException("", e);
        }
    }

    void ensureDocumentStarted() {
        if (context.isEmpty()) {
            emit(STREAM_START);
            emit(settings.isExplicitStart() ? DOCUMENT_START_EXPLICIT : DOCUMENT_START_DEFAULT);
        }
    }

    void assertObjectContext() {
        if (context.isEmpty() || context.peekFirst() != ContextType.OBJECT) {
            throw new JsonGenerationException("Not in object context");
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
        final ScalarStyle style;

        if (forcePlain) {
            scalarValue = String.valueOf(value);
            style = ScalarStyle.PLAIN;
        } else {
            scalarValue = String.valueOf(value);
            boolean containsNewLine = scalarValue.indexOf('\n') > -1;
            boolean containsDoubleQuote = scalarValue.indexOf('"') > -1;
            boolean containsSingleQuote = scalarValue.indexOf('\'') > -1;

            if (containsNewLine) {
                // TODO Allow for folded scalar style via configuration
                style = ScalarStyle.LITERAL;
            } else if (containsDoubleQuote && containsSingleQuote) {
                style = ScalarStyle.LITERAL;
            } else if (containsDoubleQuote) {
                style = ScalarStyle.SINGLE_QUOTED;
            } else if (containsSingleQuote) {
                style = ScalarStyle.DOUBLE_QUOTED;
            } else if (quoteCheck.test(scalarValue)) {
                style = ScalarStyle.SINGLE_QUOTED;
            } else {
                style = ScalarStyle.PLAIN;
            }
        }

        emit(new ScalarEvent(null, null, omitTags, scalarValue, null, null, style));
    }

    @Override
    public JsonGenerator writeStartObject() {
        ensureDocumentStarted();
        context.push(ContextType.OBJECT);
        emit(MAPPING_START);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.OBJECT);
        emit(MAPPING_START);
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
        emit(SEQUENCE_START);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.ARRAY);
        emit(SEQUENCE_START);
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
            emit(MAPPING_END);
        } else {
            emit(SEQUENCE_END);
        }

        if (this.context.isEmpty()) {
            emit(settings.isExplicitEnd() ? DOCUMENT_END_EXPLICIT : DOCUMENT_END_DEFAULT);
            emit(STREAM_END);
        }

        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        Objects.requireNonNull(value, VALUE);

        switch (value.getValueType()) {
        case NULL:
            emitScalar((Object) null);
            break;

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
        emitScalar(value);
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
        try {
            flush();
            writer.close();
        } catch (IOException e) {
            throw new JsonException("Exception closing YAML output", e);
        }

        if (!context.isEmpty()) {
            throw new JsonGenerationException("Output YAML is incomplete");
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            // TODO: exception message
            throw new JsonException("", e);
        }
    }

    enum ContextType {
        ARRAY,
        OBJECT
    }

}
