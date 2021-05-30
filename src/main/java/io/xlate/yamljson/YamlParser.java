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

import java.io.Reader;
import java.util.Iterator;
import java.util.function.Function;

import org.snakeyaml.engine.v2.exceptions.Mark;

final class YamlParser extends AbstractYamlParser<org.snakeyaml.engine.v2.events.Event, Mark> {

    YamlParser(Iterator<org.snakeyaml.engine.v2.events.Event> yamlEvents, Reader yamlReader) {
        super(yamlEvents, yamlReader);
    }

    @Override
    public long getLineNumber() {
        return currentEventPosition(mark -> mark.getLine() + 1);
    }

    @Override
    public long getColumnNumber() {
        return currentEventPosition(mark -> mark.getColumn() + 1);
    }

    @Override
    public long getStreamOffset() {
        return currentEventPosition(Mark::getIndex);
    }

    @Override
    protected long currentEventPosition(Function<Mark, Integer> mapper) {
        if (currentYamlEvent != null) {
            return currentYamlEvent.getStartMark().map(mapper).orElse(-1);
        }

        return -1;
    }

    @Override
    protected String getEventId(org.snakeyaml.engine.v2.events.Event event) {
        return event.getEventId().toString();
    }

    @Override
    protected String getValue(org.snakeyaml.engine.v2.events.Event event) {
        return org.snakeyaml.engine.v2.events.ScalarEvent.class.cast(event).getValue();
    }

    @Override
    protected boolean isPlain(org.snakeyaml.engine.v2.events.Event event) {
        return org.snakeyaml.engine.v2.events.ScalarEvent.class.cast(event).isPlain();
    }

}
