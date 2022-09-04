/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.xlate.yamljson;

import java.io.Reader;
import java.util.Iterator;

final class SnakeYamlEngineParser extends YamlParser<org.snakeyaml.engine.v2.events.Event, org.snakeyaml.engine.v2.exceptions.Mark> {

    SnakeYamlEngineParser(Iterator<org.snakeyaml.engine.v2.events.Event> yamlEvents, Reader yamlReader) {
        super(yamlEvents, yamlReader);
    }

    @Override
    protected org.snakeyaml.engine.v2.exceptions.Mark getMark() {
        if (currentYamlEvent != null) {
            return currentYamlEvent.getStartMark().orElse(null);
        }

        return null;
    }

    @Override
    protected int getMarkLine(org.snakeyaml.engine.v2.exceptions.Mark mark) {
        return mark.getLine();
    }

    @Override
    protected int getMarkColumn(org.snakeyaml.engine.v2.exceptions.Mark mark) {
        return mark.getColumn();
    }

    @Override
    protected int getMarkIndex(org.snakeyaml.engine.v2.exceptions.Mark mark) {
        return mark.getIndex();
    }

    @Override
    protected String getEventId(org.snakeyaml.engine.v2.events.Event event) {
        return event.getEventId().toString();
    }

    @Override
    protected String getAnchor(org.snakeyaml.engine.v2.events.Event event) {
        if (event instanceof org.snakeyaml.engine.v2.events.NodeEvent) {
            if (event instanceof org.snakeyaml.engine.v2.events.AliasEvent) {
                // Anchors associated with an alias not supported
                return null;
            }

            return ((org.snakeyaml.engine.v2.events.NodeEvent) event)
                    .getAnchor()
                    .map(org.snakeyaml.engine.v2.common.Anchor::getValue)
                    .orElse(null);
        }
        return null;
    }

    @Override
    protected String getAlias(org.snakeyaml.engine.v2.events.Event event) {
        if (event instanceof org.snakeyaml.engine.v2.events.AliasEvent) {
            return ((org.snakeyaml.engine.v2.events.AliasEvent) event).getAlias().getValue();
        }
        return null;
    }

    @Override
    protected String getValue(org.snakeyaml.engine.v2.events.Event event) {
        return ((org.snakeyaml.engine.v2.events.ScalarEvent) event).getValue();
    }

    @Override
    protected boolean isPlain(org.snakeyaml.engine.v2.events.Event event) {
        return ((org.snakeyaml.engine.v2.events.ScalarEvent) event).isPlain();
    }

}
