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

final class SnakeYamlParser extends YamlParser<org.yaml.snakeyaml.events.Event, org.yaml.snakeyaml.error.Mark> {

    SnakeYamlParser(Iterator<org.yaml.snakeyaml.events.Event> yamlEvents, Reader yamlReader) {
        super(yamlEvents, yamlReader);
    }

    @Override
    protected org.yaml.snakeyaml.error.Mark getMark() {
        if (currentYamlEvent != null) {
            return currentYamlEvent.getStartMark();
        }

        return null;
    }

    @Override
    protected int getMarkLine(org.yaml.snakeyaml.error.Mark mark) {
        return mark.getLine();
    }

    @Override
    protected int getMarkColumn(org.yaml.snakeyaml.error.Mark mark) {
        return mark.getColumn();
    }

    @Override
    protected int getMarkIndex(org.yaml.snakeyaml.error.Mark mark) {
        return mark.getIndex();
    }

    @Override
    protected String getEventId(org.yaml.snakeyaml.events.Event event) {
        return event.getEventId().toString();
    }

    @Override
    protected String getAnchor(org.yaml.snakeyaml.events.Event event) {
        if (event instanceof org.yaml.snakeyaml.events.NodeEvent) {
            if (event instanceof org.yaml.snakeyaml.events.AliasEvent) {
                // Anchors associated with an alias not supported
                return null;
            }

            return ((org.yaml.snakeyaml.events.NodeEvent) event).getAnchor();
        }
        return null;
    }

    @Override
    protected String getAlias(org.yaml.snakeyaml.events.Event event) {
        if (event instanceof org.yaml.snakeyaml.events.AliasEvent) {
            return ((org.yaml.snakeyaml.events.AliasEvent) event).getAnchor();
        }
        return null;
    }

    @Override
    protected String getValue(org.yaml.snakeyaml.events.Event event) {
        return ((org.yaml.snakeyaml.events.ScalarEvent) event).getValue();
    }

    @Override
    protected boolean isPlain(org.yaml.snakeyaml.events.Event event) {
        return ((org.yaml.snakeyaml.events.ScalarEvent) event).isPlain();
    }

}
