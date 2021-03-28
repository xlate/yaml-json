package io.xlate.yamljson;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.common.SpecVersion;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.events.DocumentEndEvent;
import org.snakeyaml.engine.v2.events.DocumentStartEvent;
import org.snakeyaml.engine.v2.events.Event;
import org.snakeyaml.engine.v2.events.ImplicitTuple;
import org.snakeyaml.engine.v2.events.MappingEndEvent;
import org.snakeyaml.engine.v2.events.MappingStartEvent;
import org.snakeyaml.engine.v2.events.ScalarEvent;
import org.snakeyaml.engine.v2.events.SequenceEndEvent;
import org.snakeyaml.engine.v2.events.SequenceStartEvent;
import org.snakeyaml.engine.v2.events.StreamEndEvent;
import org.snakeyaml.engine.v2.events.StreamStartEvent;

import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerationException;
import jakarta.json.stream.JsonGenerator;

class YamlGenerator implements JsonGenerator {

    static final String VALUE = "value";

    static final SpecVersion yamlVersion = new SpecVersion(1, 2);
    static final ImplicitTuple omitTags = new ImplicitTuple(true, true);

    static final Event STREAM_START = new StreamStartEvent();
    static final Event STREAM_END = new StreamEndEvent();

    // TODO Configurable explicit document start and end
    static final Event DOCUMENT_START = new DocumentStartEvent(false, Optional.empty(), Collections.emptyMap());
    static final Event DOCUMENT_END = new DocumentEndEvent(false);

    static final Event MAPPING_START = new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO);
    static final Event MAPPING_END = new MappingEndEvent();

    static final Event SEQUENCE_START = new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO);
    static final Event SEQUENCE_END = new SequenceEndEvent();

    static final StringQuotingChecker quoteChecker = new StringQuotingChecker();

    final Closeable writer;
    final StreamDataWriter yamlWriter;
    final Emitter emitter;
    final Deque<ContextType> context = new ArrayDeque<>();

    YamlGenerator(DumpSettings settings, Writer writer) {
        this.writer = writer;
        this.yamlWriter = new YamlWriterStream(writer);
        this.emitter = new Emitter(settings, yamlWriter);
    }

    void ensureDocumentStarted() {
        if (context.isEmpty()) {
            emitter.emit(STREAM_START);
            emitter.emit(DOCUMENT_START);
        }
    }

    void assertObjectContext() {
        if (context.isEmpty() || context.peekFirst() != ContextType.OBJECT) {
            throw new JsonGenerationException("Not in object context");
        }
    }

    void emitScalar(Object value) {
        emitScalar(value, quoteChecker::needToQuoteValue);
    }

    void emitScalar(Object value, boolean forcePlain) {
        emitScalar(value, forcePlain, quoteChecker::needToQuoteValue);
    }

    void emitScalar(Object value, Predicate<String> quoteCheck) {
        emitScalar(value, false, quoteCheck);
    }

    void emitScalar(Object value, boolean forcePlain, Predicate<String> quoteCheck) {
        final String scalarValue;
        final ScalarStyle style;

        if (forcePlain || value == null) {
            // TODO Allow configuration of null output - null/Null/NULL/~
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

        emitter.emit(new ScalarEvent(Optional.empty(), Optional.empty(), omitTags, scalarValue, style));
    }

    @Override
    public JsonGenerator writeStartObject() {
        ensureDocumentStarted();
        context.push(ContextType.OBJECT);
        emitter.emit(MAPPING_START);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.OBJECT);
        emitter.emit(MAPPING_START);
        return this;
    }

    @Override
    public JsonGenerator writeKey(String name) {
        Objects.requireNonNull(name, "name");
        assertObjectContext();
        emitScalar(name, quoteChecker::needToQuoteName);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        ensureDocumentStarted();
        context.push(ContextType.ARRAY);
        emitter.emit(SEQUENCE_START);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        Objects.requireNonNull(name, "name");
        writeKey(name);
        context.push(ContextType.ARRAY);
        emitter.emit(SEQUENCE_START);
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
            emitter.emit(MAPPING_END);
        } else {
            emitter.emit(SEQUENCE_END);
        }

        if (this.context.isEmpty()) {
            emitter.emit(DOCUMENT_END);
            emitter.emit(STREAM_END);
        }

        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        Objects.requireNonNull(value, VALUE);

        switch (value.getValueType()) {
        case NULL:
            emitScalar(null);
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
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        Objects.requireNonNull(value, VALUE);
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator write(int value) {
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator write(boolean value) {
        emitScalar(value, true);
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        emitScalar(null, true);
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
        this.yamlWriter.flush();
    }

    enum ContextType {
        ARRAY,
        OBJECT
    }

    class YamlWriterStream implements StreamDataWriter {
        final Writer writer;

        YamlWriterStream(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void write(String str) {
            try {
                writer.write(str);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void write(String str, int off, int len) {
            try {
                writer.write(str, off, len);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void flush() {
            try {
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
