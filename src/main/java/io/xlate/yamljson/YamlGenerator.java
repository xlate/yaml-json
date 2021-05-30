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
import java.util.Collections;
import java.util.Optional;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
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
import jakarta.json.stream.JsonGenerator;

class YamlGenerator extends AbstractYamlGenerator<Event, ScalarStyle> implements JsonGenerator {

    static final ImplicitTuple omitTags = new ImplicitTuple(true, true);

    static final Event STREAM_START = new StreamStartEvent();
    static final Event STREAM_END = new StreamEndEvent();

    static final Event DOCUMENT_START_DEFAULT = new DocumentStartEvent(false, Optional.empty(), Collections.emptyMap());
    static final Event DOCUMENT_START_EXPLICIT = new DocumentStartEvent(true, Optional.empty(), Collections.emptyMap());

    static final Event DOCUMENT_END_DEFAULT = new DocumentEndEvent(false);
    static final Event DOCUMENT_END_EXPLICIT = new DocumentEndEvent(true);

    static final Event MAPPING_START = new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO);
    static final Event MAPPING_END = new MappingEndEvent();

    static final Event SEQUENCE_START = new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO);
    static final Event SEQUENCE_END = new SequenceEndEvent();

    static final StringQuotingChecker quoteChecker = new StringQuotingChecker();

    final StreamDataWriter yamlWriter;
    final DumpSettings settings;
    final Emitter emitter;

    YamlGenerator(DumpSettings settings, Writer writer) {
        super(writer);
        this.yamlWriter = new YamlWriterStream(writer);
        this.settings = settings;
        this.emitter = new Emitter(settings, yamlWriter);
    }

    @Override
    protected void emit(Event event) {
        try {
            emitter.emit(event);
        } catch (UncheckedIOException e) {
            // TODO: exception message
            throw new JsonException("", e.getCause());
        }
    }

    @Override
    protected boolean isExplicitStart() {
        return settings.isExplicitStart();
    }

    @Override
    protected boolean isExplicitEnd() {
        return settings.isExplicitEnd();
    }

    @Override
    protected Event getStreamStart() {
        return STREAM_START;
    }

    @Override
    protected Event getStreamEnd() {
        return STREAM_END;
    }

    @Override
    public Event getDocumentStartExplicit() {
        return DOCUMENT_START_EXPLICIT;
    }

    @Override
    public Event getDocumentStartDefault() {
        return DOCUMENT_START_DEFAULT;
    }

    @Override
    public Event getDocumentEndExplicit() {
        return DOCUMENT_END_EXPLICIT;
    }

    @Override
    public Event getDocumentEndDefault() {
        return DOCUMENT_END_DEFAULT;
    }

    @Override
    public Event getMappingStart() {
        return MAPPING_START;
    }

    @Override
    public Event getMappingEnd() {
        return MAPPING_END;
    }

    @Override
    public Event getSequenceStart() {
        return SEQUENCE_START;
    }

    @Override
    public Event getSequenceEnd() {
        return SEQUENCE_END;
    }

    @Override
    protected ScalarStyle getPlainStyle() {
        return ScalarStyle.PLAIN;
    }

    @Override
    protected ScalarStyle getLiteralStyle() {
        return ScalarStyle.LITERAL;
    }

    @Override
    protected ScalarStyle getSingleQuotedStyle() {
        return ScalarStyle.SINGLE_QUOTED;
    }

    @Override
    protected ScalarStyle getDoubleQuotedStyle() {
        return ScalarStyle.DOUBLE_QUOTED;
    }

    @Override
    protected Event buildScalarEvent(String scalarValue, ScalarStyle style) {
        return new ScalarEvent(Optional.empty(), Optional.empty(), omitTags, scalarValue, style);
    }
    @Override
    public void flush() {
        this.yamlWriter.flush();
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
