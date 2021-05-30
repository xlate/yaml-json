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

import org.yaml.snakeyaml.error.Mark;

final class YamlParser1_1 extends AbstractYamlParser<org.yaml.snakeyaml.events.Event, Mark> {

    YamlParser1_1(Iterator<org.yaml.snakeyaml.events.Event> yamlEvents, Reader yamlReader) {
        super(yamlEvents, yamlReader);
    }

    @Override
    protected Mark getMark() {
        if (currentYamlEvent != null) {
            return currentYamlEvent.getStartMark();
        }

        return null;
    }

    @Override
    protected int getMarkLine(Mark mark) {
        return mark.getLine();
    }

    @Override
    protected int getMarkColumn(Mark mark) {
        return mark.getColumn();
    }

    @Override
    protected int getMarkIndex(Mark mark) {
        return mark.getIndex();
    }

    @Override
    protected String getEventId(org.yaml.snakeyaml.events.Event event) {
        return event.getEventId().toString();
    }

    @Override
    protected String getValue(org.yaml.snakeyaml.events.Event event) {
        return org.yaml.snakeyaml.events.ScalarEvent.class.cast(event).getValue();
    }

    @Override
    protected boolean isPlain(org.yaml.snakeyaml.events.Event event) {
        return org.yaml.snakeyaml.events.ScalarEvent.class.cast(event).isPlain();
    }

}
