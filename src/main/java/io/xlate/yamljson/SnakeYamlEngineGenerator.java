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
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

class SnakeYamlEngineGenerator extends YamlGenerator<Event, ScalarStyle> implements JsonGenerator {

    static final ImplicitTuple omitTags = new ImplicitTuple(true, true);

    static final Map<EventType, Event> EVENTS = new EnumMap<>(EventType.class);
    static final Map<StyleType, ScalarStyle> STYLES = new EnumMap<>(StyleType.class);

    static {
        EVENTS.put(EventType.STREAM_START, new StreamStartEvent());
        EVENTS.put(EventType.STREAM_END, new StreamEndEvent());
        EVENTS.put(EventType.DOCUMENT_START_DEFAULT, new DocumentStartEvent(false, Optional.empty(), Collections.emptyMap()));
        EVENTS.put(EventType.DOCUMENT_START_EXPLICIT, new DocumentStartEvent(true, Optional.empty(), Collections.emptyMap()));
        EVENTS.put(EventType.DOCUMENT_END_DEFAULT, new DocumentEndEvent(false));
        EVENTS.put(EventType.DOCUMENT_END_EXPLICIT, new DocumentEndEvent(true));
        EVENTS.put(EventType.MAPPING_START, new MappingStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO));
        EVENTS.put(EventType.MAPPING_END, new MappingEndEvent());
        EVENTS.put(EventType.SEQUENCE_START, new SequenceStartEvent(Optional.empty(), Optional.empty(), true, FlowStyle.AUTO));
        EVENTS.put(EventType.SEQUENCE_END, new SequenceEndEvent());

        Stream.of(StyleType.values()).forEach(v -> STYLES.put(v, ScalarStyle.valueOf(v.toString())));
    }

    final StreamDataWriter yamlWriter;
    final DumpSettings settings;
    final Emitter emitter;

    SnakeYamlEngineGenerator(DumpSettings settings, Writer writer) {
        super(EVENTS, STYLES, writer, settings.isExplicitStart(), settings.isExplicitEnd());
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
