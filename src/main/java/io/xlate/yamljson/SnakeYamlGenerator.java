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
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

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
import jakarta.json.stream.JsonGenerator;

class SnakeYamlGenerator extends AbstractYamlGenerator<Event, ScalarStyle> implements JsonGenerator {

    static final ImplicitTuple omitTags = new ImplicitTuple(true, true);

    static final Map<EventType, Event> EVENTS = new EnumMap<>(EventType.class);
    static final Map<StyleType, ScalarStyle> STYLES = new EnumMap<>(StyleType.class);

    static {
        EVENTS.put(EventType.STREAM_START, new StreamStartEvent(null, null));
        EVENTS.put(EventType.STREAM_END, new StreamEndEvent(null, null));
        EVENTS.put(EventType.DOCUMENT_START_DEFAULT, new DocumentStartEvent(null, null, false, null, Collections.emptyMap()));
        EVENTS.put(EventType.DOCUMENT_START_EXPLICIT, new DocumentStartEvent(null, null, true, null, Collections.emptyMap()));
        EVENTS.put(EventType.DOCUMENT_END_DEFAULT, new DocumentEndEvent(null, null, false));
        EVENTS.put(EventType.DOCUMENT_END_EXPLICIT, new DocumentEndEvent(null, null, true));
        EVENTS.put(EventType.MAPPING_START, new MappingStartEvent(null, null, true, null, null, FlowStyle.AUTO));
        EVENTS.put(EventType.MAPPING_END, new MappingEndEvent(null, null));
        EVENTS.put(EventType.SEQUENCE_START, new SequenceStartEvent(null, null, true, null, null, FlowStyle.AUTO));
        EVENTS.put(EventType.SEQUENCE_END, new SequenceEndEvent(null, null));

        Stream.of(StyleType.values()).forEach(v -> STYLES.put(v, ScalarStyle.valueOf(v.toString())));
    }

    final DumperOptions settings;
    final Emitter emitter;

    SnakeYamlGenerator(DumperOptions settings, Writer writer) {
        super(EVENTS, STYLES, writer, settings.isExplicitStart(), settings.isExplicitEnd());
        this.settings = settings;
        this.emitter = new Emitter(writer, settings);
    }

    @Override
    protected void emit(Event event) {
        try {
            emitter.emit(event);
        } catch (IOException e) {
            // TODO: exception message
            throw new JsonException("", e);
        }
    }

    @Override
    protected Event buildScalarEvent(String scalarValue, ScalarStyle style) {
        return new ScalarEvent(null, null, omitTags, scalarValue, null, null, style);
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

}
