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

abstract class AbstractYamlGenerator<E, S> implements JsonGenerator {

    enum EventType {
        STREAM_START,
        STREAM_END,
        DOCUMENT_START_DEFAULT,
        DOCUMENT_START_EXPLICIT,
        DOCUMENT_END_DEFAULT,
        DOCUMENT_END_EXPLICIT,
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

    static final String VALUE = "value";

    static final StringQuotingChecker quoteChecker = new StringQuotingChecker();

    protected final Map<EventType, E> eventTypes;
    protected final Map<StyleType, S> styleTypes;
    protected final Writer writer;
    final boolean explicitStart;
    final boolean explicitEnd;
    final Deque<ContextType> context = new ArrayDeque<>();

    AbstractYamlGenerator(Map<EventType, E> eventTypes, Map<StyleType, S> styleTypes, Writer writer, boolean explicitStart, boolean explicitEnd) {
        this.eventTypes = eventTypes;
        this.styleTypes = styleTypes;
        this.writer = writer;
        this.explicitStart = explicitStart;
        this.explicitEnd = explicitEnd;
    }

    protected abstract void emit(E event);

    void ensureDocumentStarted() {
        if (context.isEmpty()) {
            emit(eventTypes.get(EventType.STREAM_START));
            emit(explicitStart ?
                eventTypes.get(EventType.DOCUMENT_START_EXPLICIT) :
                    eventTypes.get(EventType.DOCUMENT_START_DEFAULT));
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

    protected abstract E buildScalarEvent(String scalarValue, S style);

    void emitScalar(Object value, boolean forcePlain, Predicate<String> quoteCheck) {
        final String scalarValue;
        final S style;

        if (forcePlain) {
            scalarValue = String.valueOf(value);
            style = styleTypes.get(StyleType.PLAIN);
        } else {
            scalarValue = String.valueOf(value);
            boolean containsNewLine = scalarValue.indexOf('\n') > -1;
            boolean containsDoubleQuote = scalarValue.indexOf('"') > -1;
            boolean containsSingleQuote = scalarValue.indexOf('\'') > -1;

            if (containsNewLine) {
                // TODO Allow for folded scalar style via configuration
                style = styleTypes.get(StyleType.LITERAL);
            } else if (containsDoubleQuote && containsSingleQuote) {
                style = styleTypes.get(StyleType.LITERAL);
            } else if (containsDoubleQuote) {
                style = styleTypes.get(StyleType.SINGLE_QUOTED);
            } else if (containsSingleQuote) {
                style = styleTypes.get(StyleType.DOUBLE_QUOTED);
            } else if (quoteCheck.test(scalarValue)) {
                style = styleTypes.get(StyleType.SINGLE_QUOTED);
            } else {
                style = styleTypes.get(StyleType.PLAIN);
            }
        }

        emit(buildScalarEvent(scalarValue, style));
    }

    @Override
    public JsonGenerator writeStartObject() {
        ensureDocumentStarted();
        context.push(ContextType.OBJECT);
        emit(eventTypes.get(EventType.MAPPING_START));
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.OBJECT);
        emit(eventTypes.get(EventType.MAPPING_START));
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
        emit(eventTypes.get(EventType.SEQUENCE_START));
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.ARRAY);
        emit(eventTypes.get(EventType.SEQUENCE_START));
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
            emit(eventTypes.get(EventType.MAPPING_END));
        } else {
            emit(eventTypes.get(EventType.SEQUENCE_END));
        }

        if (this.context.isEmpty()) {
            emit(explicitEnd ?
                eventTypes.get(EventType.DOCUMENT_END_EXPLICIT) :
                    eventTypes.get(EventType.DOCUMENT_END_DEFAULT));
            emit(eventTypes.get(EventType.STREAM_END));
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

    enum ContextType {
        ARRAY,
        OBJECT
    }

}
