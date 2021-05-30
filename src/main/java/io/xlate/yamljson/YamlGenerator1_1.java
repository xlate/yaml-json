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

class YamlGenerator1_1 extends AbstractYamlGenerator<Event, ScalarStyle> implements JsonGenerator {

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

    final DumperOptions settings;
    final Emitter emitter;

    YamlGenerator1_1(DumperOptions settings, Writer writer) {
        super(writer);
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
