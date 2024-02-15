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
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;

abstract class YamlParser<E, M> implements JsonParser {

    enum NumberType {
        INTEGER,
        FLOAT,
        POSITIVE_INFINITY,
        NEGATIVE_INFINITY,
        NAN,
        OCTAL(2, 8), // 2 = skip leading '0o'
        HEXADECIMAL(2, 16), // 2 = skip leading '0x'
        NONE;

        int start;
        int radix;

        NumberType(int start, int radix) {
            this.start = start;
            this.radix = radix;
        }

        NumberType() {
            this(0, 10);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(YamlParser.class.getName());

    static final String MSG_EXCEPTION = "Exception reading the YAML stream as JSON";
    static final String MSG_UNEXPECTED = "Unexpected jsonEvent reached parsing YAML: ";

    // Support all the values from the Core Schema (https://yaml.org/spec/1.2/spec.html#id2804923)
    static final Set<String> VALUES_NULL = Set.of("null", "Null", "NULL", "~");
    static final Set<String> VALUES_TRUE = Set.of("true", "True", "TRUE");
    static final Set<String> VALUES_FALSE = Set.of("false", "False", "FALSE");
    // @formatter:off
    static final Set<String> VALUES_INFINITY = Set.of(YamlNumbers.CANONICAL_POSITIVE_INFINITY, ".Inf", ".INF", // Positive infinity
                                                      "+.inf", "+.Inf", "+.INF", // Explicit positive infinity
                                                      YamlNumbers.CANONICAL_NEGATIVE_INFINITY, "-.Inf", "-.INF"); // Negative Infinity
    // @formatter:on
    static final Set<String> VALUES_NAN = Set.of(YamlNumbers.CANONICAL_NAN, ".NaN", ".NAN");

    static final BigDecimal UNSET_NUMBER = new BigDecimal(0);
    static final YamlLocation UNKNOWN_LOCATION = new YamlLocation(-1, -1, -1);

    final JsonProvider jsonProvider = JsonProvider.provider();
    final Reader yamlSource;
    final Iterator<E> yamlEvents;
    final Map<String, ?> properties;
    final long maxAliasExpansionSize;

    final Deque<E> yamlEventQueue = new ArrayDeque<>();
    final Deque<Boolean> aliasExpansionQueue = new ArrayDeque<>();
    final Deque<Event> jsonEventQueue = new ArrayDeque<>();
    final Deque<String> valueQueue = new ArrayDeque<>();
    final Deque<NumberType> numberTypeQueue = new ArrayDeque<>();
    final Deque<BigDecimal> numberQueue = new ArrayDeque<>();

    final DecimalFormat decimalParser = new DecimalFormat();
    final ParsePosition decimalPosition = new ParsePosition(0);

    E currentYamlEvent;
    Event currentEvent;
    NumberType currentNumberType;
    String currentValue;
    BigDecimal currentNumber;

    final Boolean[] valueIsKey = new Boolean[200];
    final List<Event> eventStack = new ArrayList<>();
    int depth = -1;
    final Deque<AnchorMetadata> anchorStack = new ArrayDeque<>();

    static class AnchorMetadata {
        final String name;
        final int startDepth;

        public AnchorMetadata(String name, int startDepth) {
            this.name = name;
            this.startDepth = startDepth;
        }
    }

    static class AnchoredEvent<E> {
        final E yamlEvent;

        AnchoredEvent(E yamlEvent) {
            this.yamlEvent = yamlEvent;
        }
    }

    static class AnchoredDataEvent<E> extends AnchoredEvent<E> {
        final Event jsonEvent;
        final String value;
        final NumberType numberType;
        final BigDecimal numberValue;

        AnchoredDataEvent(E yamlEvent, Event jsonEvent, String value, NumberType numberType, BigDecimal numberValue) {
            super(yamlEvent);
            this.jsonEvent = jsonEvent;
            this.value = value;
            this.numberType = numberType;
            this.numberValue = numberValue;
        }
    }

    final Map<String, List<AnchoredEvent<E>>> anchoredEvents = new HashMap<>();

    YamlParser(Iterator<E> yamlEvents, Reader yamlReader, Map<String, ?> properties) {
        this.yamlEvents = yamlEvents;
        this.yamlSource = yamlReader;
        this.properties = properties;
        this.maxAliasExpansionSize = (Long) properties.get(Yaml.Settings.LOAD_MAX_ALIAS_EXPANSION_SIZE);
    }

    void advanceEvent() {
        currentYamlEvent = yamlEventQueue.remove();
        currentEvent = jsonEventQueue.remove();
        currentValue = valueQueue.remove();
        currentNumberType = numberTypeQueue.remove();
        currentNumber = numberQueue.remove();

        if (currentNumberType == NumberType.NONE) {
            currentNumber = null;
        }

        String alias = getAlias(currentYamlEvent);
        boolean aliasExpansion = aliasExpansionQueue.remove();

        if (!aliasExpansion) {
            addAnchoredEvent(currentYamlEvent, alias);
        }

        if (alias != null) {
            Event jsonEventOverride = currentEvent != Event.VALUE_NULL ? currentEvent : null;
            List<AnchoredEvent<E>> events = anchoredEvents.get(alias);
            ListIterator<AnchoredEvent<E>> iterator = events.listIterator(events.size());

            while (iterator.hasPrevious()) {
                enqueue(iterator.previous(), jsonEventOverride);
            }

            advanceEvent();
        }
    }

    void addAnchoredEvent(E yamlEvent, String alias) {
        if (!anchorStack.isEmpty()) {
            Iterator<AnchorMetadata> iter = anchorStack.iterator();

            while (iter.hasNext()) {
                AnchorMetadata anchorMeta = iter.next();

                if (anchorMeta.startDepth <= depth) {
                    if (alias != null) {
                        addAnchoredAliasEvent(anchorMeta.name, yamlEvent);
                    } else {
                        addAnchoredDataEvent(anchorMeta.name, yamlEvent, currentEvent, currentValue, currentNumberType, currentNumber);
                    }
                }
            }
        }
    }

    void addAnchoredAliasEvent(String anchor, E yamlEvent) {
        addAnchoredEvent(anchor, new AnchoredEvent<>(yamlEvent));
    }

    void addAnchoredDataEvent(String anchor, E yamlEvent, Event jsonEvent, String value, NumberType numberType, BigDecimal numberValue) {
        addAnchoredEvent(anchor, new AnchoredDataEvent<>(yamlEvent, jsonEvent, value, numberType, numberValue));
    }

    void addAnchoredEvent(String anchor, AnchoredEvent<E> anchored) {
        anchoredEvents.get(anchor).add(anchored);
    }

    BigDecimal parseNumber(NumberType numberType, String text) {
        BigDecimal parsed = null;

        switch (numberType) {
        case OCTAL:
        case INTEGER:
        case HEXADECIMAL:
            long parsedValue = Long.parseLong(text.substring(numberType.start), numberType.radix);
            parsed = BigDecimal.valueOf(parsedValue);
            break;
        case FLOAT:
            decimalPosition.setIndex(0);
            decimalParser.setParseBigDecimal(true);
            if (text.indexOf('e') >= 0) {
                text = text.replace('e', 'E');
            }
            parsed = (BigDecimal) decimalParser.parse(text, decimalPosition);
            break;
        default:
            break;
        }

        return parsed;
    }

    BigDecimal numberValue(Event jsonEvent, NumberType numberType, String value, BigDecimal numberValue) {
        if (jsonEvent == Event.VALUE_NUMBER) {
            numberValue = Objects.requireNonNullElseGet(numberValue, () -> parseNumber(numberType, value));
            return Objects.requireNonNullElse(numberValue, UNSET_NUMBER);
        }
        return UNSET_NUMBER;
    }

    void enqueueFirst(E yamlEvent, Boolean aliasExpansion, Event jsonEvent, NumberType numberType, String value, BigDecimal numberValue) {
        yamlEventQueue.addFirst(yamlEvent);
        aliasExpansionQueue.addFirst(aliasExpansion);
        jsonEventQueue.addFirst(jsonEvent);
        numberTypeQueue.addFirst(numberType);
        valueQueue.addFirst(value);
        numberQueue.addFirst(numberValue(jsonEvent, numberType, value, numberValue));
    }

    void enqueue(AnchoredEvent<E> anchor, Event jsonEventOverride) {
        String alias = getAlias(anchor.yamlEvent);

        if (alias != null) {
            enqueueFirst(anchor.yamlEvent, Boolean.TRUE, Event.VALUE_NULL, NumberType.NONE, "", UNSET_NUMBER);
        } else {
            AnchoredDataEvent<E> dataEvent = (AnchoredDataEvent<E>) anchor;
            Event jsonEvent = Objects.requireNonNullElse(jsonEventOverride, dataEvent.jsonEvent);
            enqueueFirst(anchor.yamlEvent, Boolean.TRUE, jsonEvent, dataEvent.numberType, dataEvent.value, dataEvent.numberValue);
        }
    }

    void enqueue(E yamlEvent, Event jsonEvent, NumberType numberType, String value, BigDecimal numberValue) {
        yamlEventQueue.addLast(yamlEvent);
        aliasExpansionQueue.addLast(Boolean.FALSE);
        jsonEventQueue.addLast(jsonEvent);
        numberTypeQueue.addLast(numberType);
        valueQueue.addLast(value);
        numberQueue.addLast(numberValue(jsonEvent, numberType, value, numberValue));
    }

    void enqueueString(E yamlEvent, Event jsonEvent, String value) {
        enqueue(yamlEvent, jsonEvent, NumberType.NONE, value, UNSET_NUMBER);
    }

    void enqueueNumber(E yamlEvent, Event jsonEvent, NumberType numberType, String value) {
        enqueue(yamlEvent, jsonEvent, numberType, value, null);
    }

    void enqueueConstantOrString(E yamlEvent, String dataText, Set<String> constants, Event constantType) {
        final Event dataEvent = constants.contains(dataText) ? constantType : Event.VALUE_STRING;
        enqueueString(yamlEvent, dataEvent, dataText);
    }

    void enqueueNumberOrString(E yamlEvent, String dataText) {
        if (YamlNumbers.isInteger(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_NUMBER, NumberType.INTEGER, dataText);
        } else if (YamlNumbers.isFloat(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_NUMBER, NumberType.FLOAT, dataText);
        } else {
            enqueueString(yamlEvent, Event.VALUE_STRING, dataText);
        }
    }

    void enqueueZeroPrefixedValue(E yamlEvent, String dataText) {
        if (YamlNumbers.isOctal(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_NUMBER, NumberType.OCTAL, dataText);
        } else if (YamlNumbers.isHexadecimal(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_NUMBER, NumberType.HEXADECIMAL, dataText);
        } else {
            enqueueNumberOrString(yamlEvent, dataText);
        }
    }

    void enqueueDotPrefixedValue(E yamlEvent, String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_STRING, NumberType.POSITIVE_INFINITY, dataText);
        } else if (VALUES_NAN.contains(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_STRING, NumberType.NAN, dataText);
        } else if (YamlNumbers.isFloat(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_NUMBER, NumberType.FLOAT, dataText);
        } else {
            enqueueString(yamlEvent, Event.VALUE_STRING, dataText);
        }
    }

    void enqueuePlusPrefixedValue(E yamlEvent, String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_STRING, NumberType.POSITIVE_INFINITY, dataText);
        } else {
            enqueueNumberOrString(yamlEvent, dataText);
        }
    }

    void enqueueMinusPrefixedValue(E yamlEvent, String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueueNumber(yamlEvent, Event.VALUE_STRING, NumberType.NEGATIVE_INFINITY, dataText);
        } else {
            enqueueNumberOrString(yamlEvent, dataText);
        }
    }

    void enqueueDataElement(E yamlEvent, final String dataText) {
        switch (dataText.charAt(0)) {
        case 'n':
        case 'N':
        case '~':
            enqueueConstantOrString(yamlEvent, dataText, VALUES_NULL, Event.VALUE_NULL);
            break;

        case 't':
        case 'T':
            enqueueConstantOrString(yamlEvent, dataText, VALUES_TRUE, Event.VALUE_TRUE);
            break;

        case 'f':
        case 'F':
            enqueueConstantOrString(yamlEvent, dataText, VALUES_FALSE, Event.VALUE_FALSE);
            break;

        case '0':
            enqueueZeroPrefixedValue(yamlEvent, dataText);
            break;

        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            enqueueNumberOrString(yamlEvent, dataText);
            break;

        case '.':
            enqueueDotPrefixedValue(yamlEvent, dataText);
            break;

        case '+':
            enqueuePlusPrefixedValue(yamlEvent, dataText);
            break;

        case '-':
            enqueueMinusPrefixedValue(yamlEvent, dataText);
            break;

        default:
            enqueueString(yamlEvent, Event.VALUE_STRING, dataText);
            break;
        }
    }

    void enqueueDataElement(E yamlEvent, Boolean needKeyName) {
        final String dataText = getValue(yamlEvent);

        if (Boolean.TRUE.equals(needKeyName)) {
            enqueueString(yamlEvent, Event.KEY_NAME, dataText);
        } else if (isPlain(yamlEvent)) {
            if (dataText.isEmpty()) {
                enqueue(yamlEvent, Event.VALUE_NULL, NumberType.NONE, "", UNSET_NUMBER);
            } else {
                enqueueDataElement(yamlEvent, dataText);
            }
        } else {
            enqueueString(yamlEvent, Event.VALUE_STRING, dataText);
        }
    }

    void enqueueAlias(E yamlEvent, Boolean needKeyName) {
        String alias = getAlias(yamlEvent);

        if (!anchoredEvents.containsKey(alias)) {
            throw new IllegalStateException("Encountered alias of missing anchor");
        }

        if (Boolean.TRUE.equals(needKeyName)) {
            List<AnchoredEvent<E>> anchored = anchoredEvents.get(alias);

            if (anchored.size() != 1) {
                throw new IllegalStateException("Expected key but found alias of non-scalar anchor");
            }

            // Enqueue the alias event, specifying that the JSON event is KEY_NAME
            enqueue(yamlEvent, Event.KEY_NAME, NumberType.NONE, "", UNSET_NUMBER);
        } else {
            if (maxAliasExpansionSize < Long.MAX_VALUE) {
                long expansionSize = countExpansion(alias, maxAliasExpansionSize);

                if (expansionSize >= maxAliasExpansionSize) {
                    String message = String.format("Alias '%s' expands to too many scalars: %d", alias, expansionSize);
                    throw new JsonParsingException(message, getLocation(yamlEvent));
                }
            }

            enqueue(yamlEvent, Event.VALUE_NULL, NumberType.NONE, "", UNSET_NUMBER);
        }
    }

    long countExpansion(String alias, long limit) {
        List<AnchoredEvent<E>> anchored = anchoredEvents.get(alias);
        long count = 0;

        for (AnchoredEvent<E> event : anchored) {
            String nestedAlias = getAlias(event.yamlEvent);

            if (nestedAlias != null) {
                count += countExpansion(nestedAlias, limit);
            } else if ("Scalar".equals(getEventId(event.yamlEvent))) {
                count++;
            }

            if (count >= limit) {
                break;
            }
        }

        return count;
    }

    Boolean isKeyExpected() {
        return depth > -1 ? this.valueIsKey[depth] : null;
    }

    void incrementDepth(Event levelEvent, Boolean keyExpected) {
        if (isKeyExpected() != null) {
            this.valueIsKey[depth] = Boolean.TRUE;
        }

        depth++;
        this.valueIsKey[depth] = keyExpected;
        eventStack.add(depth, levelEvent);
    }

    void decrementDepth() {
        eventStack.remove(depth);
        depth--;
    }

    void addAnchorMetadata(String anchor) {
        if (anchor != null) {
            anchoredEvents.compute(anchor, (k, v) -> new ArrayList<>());
            anchorStack.addFirst(new AnchorMetadata(anchor, depth));
        }
    }

    void removeAnchorMetadata(E yamlEvent, Event jsonEvent) {
        AnchorMetadata anchor = anchorStack.peekFirst();
        if (anchor != null && anchor.startDepth == depth) {
            if (jsonEvent != null) {
                addAnchoredDataEvent(anchor.name, yamlEvent, jsonEvent, "", NumberType.NONE, UNSET_NUMBER);
            }
            anchorStack.removeFirst();
        }
    }

    boolean enqueueEvent(E yamlEvent) {
        LOGGER.finer(() -> "Enqueue YAML jsonEvent: " + yamlEvent);
        currentNumber = null;
        removeAnchorMetadata(yamlEvent, null);
        boolean eventFound = true;
        String eventId = getEventId(yamlEvent);

        switch (eventId) {
        case "DocumentStart":
        case "DocumentEnd":
            eventFound = false;
            break;

        case "SequenceStart":
            addAnchorMetadata(getAnchor(yamlEvent));
            incrementDepth(Event.START_ARRAY, null);
            enqueue(yamlEvent, Event.START_ARRAY, NumberType.NONE, "", UNSET_NUMBER);
            break;

        case "SequenceEnd":
            enqueue(yamlEvent, Event.END_ARRAY, NumberType.NONE, "", UNSET_NUMBER);
            decrementDepth();
            removeAnchorMetadata(yamlEvent, Event.END_ARRAY);
            break;

        case "MappingStart":
            addAnchorMetadata(getAnchor(yamlEvent));
            incrementDepth(Event.START_OBJECT, Boolean.TRUE);
            enqueue(yamlEvent, Event.START_OBJECT, NumberType.NONE, "", UNSET_NUMBER);
            break;

        case "MappingEnd":
            enqueue(yamlEvent, Event.END_OBJECT, NumberType.NONE, "", UNSET_NUMBER);
            decrementDepth();
            removeAnchorMetadata(yamlEvent, Event.END_OBJECT);
            break;

        case "Scalar": {
            addAnchorMetadata(getAnchor(yamlEvent));
            Boolean keyExpected = isKeyExpected();
            enqueueDataElement(yamlEvent, keyExpected);

            if (keyExpected != null) {
                this.valueIsKey[depth] = Boolean.valueOf(!keyExpected);
            }

            break;
        }

        case "Alias": {
            Boolean keyExpected = isKeyExpected();

            enqueueAlias(yamlEvent, keyExpected);
            addAnchorMetadata(getAnchor(yamlEvent));

            if (keyExpected != null) {
                this.valueIsKey[depth] = Boolean.valueOf(!keyExpected);
            }

            break;
        }

        case "StreamStart":
        case "StreamEnd":
            eventFound = false;
            break;
        default:
            throw new IllegalStateException("Unknown YAML event: " + eventId);
        }

        return eventFound;
    }

    void fillQueues() {
        try {
            while (yamlEvents.hasNext() && jsonEventQueue.isEmpty()) {
                LOGGER.finer(() -> "eventQueue is empty, calling yamlEvents.next()");

                if (enqueueEvent(yamlEvents.next())) {
                    break;
                }
            }
        } catch (JsonException je) {
            throw je;
        } catch (Exception re) {
            Throwable cause = re.getCause();

            if (cause instanceof IOException) {
                throw new JsonException("IOException encountered reading YAML", cause);
            }

            throw new JsonParsingException("Exception reading YAML", re, getLocation());
        }
    }

    void assertEventValueNumber() {
        final Event current = this.currentEvent;

        if (current != Event.VALUE_NUMBER) {
            throw new IllegalStateException("Unable to get number value for jsonEvent [" + current + ']');
        }
    }

    void assertEventValueString() {
        final Event current = this.currentEvent;

        switch (current) {
        case KEY_NAME:
        case VALUE_STRING:
        case VALUE_NUMBER:
            break;
        default:
            throw new IllegalStateException("Unable to get string value for jsonEvent [" + current + ']');
        }
    }

    void assertEventValue() {
        final Event current = this.currentEvent;

        switch (current) {
        case VALUE_STRING:
        case VALUE_NUMBER:
        case VALUE_FALSE:
        case VALUE_NULL:
        case VALUE_TRUE:
            break;
        default:
            throw new IllegalStateException("Current jsonEvent is not a value [" + current + ']');
        }
    }

    // JsonParser

    @Override
    public boolean hasNext() {
        if (jsonEventQueue.isEmpty()) {
            fillQueues();
        }

        return !jsonEventQueue.isEmpty();
    }

    @Override
    public Event next() {
        fillQueues();
        advanceEvent();
        return currentEvent;
    }

    // XXX: Add @Override annotation when dropping support for old javax interfaces
    @SuppressWarnings("java:S1161")
    public Event currentEvent() {
        return currentEvent;
    }

    @Override
    public void close() {
        try {
            yamlSource.close();
        } catch (IOException e) {
            throw new JsonException("Exception closing YAML source", e);
        }
    }

    @Override
    public BigDecimal getBigDecimal() {
        assertEventValueNumber();
        return currentNumber;
    }

    @Override
    public long getLong() {
        assertEventValueNumber();
        return currentNumber.longValue();
    }

    @Override
    public int getInt() {
        return (int) getLong();
    }

    @Override
    public String getString() {
        assertEventValueString();
        return this.currentValue;
    }

    @Override
    public boolean isIntegralNumber() {
        assertEventValueNumber();
        return currentNumber.scale() == 0;
    }

    public boolean isPositiveInfinity() {
        assertEventValue();
        return this.currentNumberType == NumberType.POSITIVE_INFINITY;
    }

    public boolean isNegativeInfinity() {
        assertEventValue();
        return this.currentNumberType == NumberType.NEGATIVE_INFINITY;
    }

    public boolean isInfinite() {
        return isPositiveInfinity() || isNegativeInfinity();
    }

    public boolean isNaN() {
        assertEventValue();
        return this.currentNumberType == NumberType.NAN;
    }

    @Override
    public JsonLocation getLocation() {
        return getLocation(currentYamlEvent);
    }

    JsonLocation getLocation(E yamlEvent) {
        M mark = getMark(yamlEvent);
        YamlLocation location = null;

        if (mark != null) {
            location = new YamlLocation(getMarkLine(mark) + 1L, getMarkColumn(mark) + 1L, getMarkIndex(mark));
        } else {
            location = UNKNOWN_LOCATION;
        }

        return location;
    }

    @Override
    public JsonValue getValue() {
        return getJsonValue(null);
    }

    @Override
    public JsonObject getObject() {
        return (JsonObject) getJsonValue(Event.START_OBJECT);
    }

    @Override
    public void skipObject() {
        skip(Event.START_OBJECT);
    }

    @Override
    public JsonArray getArray() {
        return (JsonArray) getJsonValue(Event.START_ARRAY);
    }

    @Override
    public void skipArray() {
        skip(Event.START_ARRAY);
    }

    JsonValue getJsonValue(Event requiredEvent) {
        Event event = currentEvent();

        if (requiredEvent != null && event != requiredEvent) {
            throw new IllegalStateException("Expected current state to be " + requiredEvent + ", but got [" + currentEvent() + ']');
        }

        Deque<Object> builders = new ArrayDeque<>();
        Deque<String> keyNames = new LinkedList<>();
        Object rootBuilder = null;
        String keyName = null;
        JsonValue rootValue = null;
        int valueDepth = 0;
        int iteration = 0;

        do {
            event = currentOrNextEvent(iteration++, event);

            switch (event) {
            case KEY_NAME:
                keyName = getString();
                break;
            case START_ARRAY:
                rootBuilder = beginStructure(builders, keyNames, keyName, jsonProvider.createArrayBuilder());
                keyName = null;
                valueDepth++;
                break;
            case START_OBJECT:
                rootBuilder = beginStructure(builders, keyNames, keyName, jsonProvider.createObjectBuilder());
                keyName = null;
                valueDepth++;
                break;
            case END_ARRAY:
            case END_OBJECT:
                endStructure(builders, keyNames);
                valueDepth--;
                break;
            default:
                JsonValue parsedValue = getParsedValue(event);

                if (builders.isEmpty()) {
                    rootValue = parsedValue;
                } else {
                    addValue(builders.peekLast(), keyName, parsedValue);
                }

                keyName = null;
                break;
            }
        } while (valueDepth > 0);

        if (rootBuilder != null) {
            rootValue = build(rootBuilder);
        } else if (rootValue == null) {
            // method was called at KEY_NAME
            Objects.requireNonNull(keyName, "Expected value for KEY_NAME, but it was null");
            rootValue = jsonProvider.createValue(keyName);
        }

        return rootValue;
    }

    void skip(Event structuralEvent) {
        final int terminalDepth = eventStack.lastIndexOf(structuralEvent);

        if (terminalDepth < 0) {
            return;
        }

        Event event = currentEvent();
        int valueDepth;
        int iteration = 0;

        /*
         * Accounting for the fact that `depth` was already modified for start/end structure events
         * and adjusting for the first pass through the loop.
         */
        switch (event) {
        case START_ARRAY:
        case START_OBJECT:
            valueDepth = depth - 1;
            break;
        case END_ARRAY:
        case END_OBJECT:
            valueDepth = depth + 1;
            break;
        default:
            valueDepth = depth;
            break;
        }

        do {
            event = currentOrNextEvent(iteration++, event);

            switch (event) {
            case START_ARRAY:
            case START_OBJECT:
                valueDepth++;
                break;

            case END_ARRAY:
            case END_OBJECT:
                valueDepth--;
                break;

            default:
                break;
            }
        } while (valueDepth >= terminalDepth);
    }

    Event currentOrNextEvent(int iteration, Event event) {
        return iteration > 0 ? next() : event;
    }

    Object beginStructure(Deque<Object> builders, Deque<String> keyNames, String keyName, Object builder) {
        builders.add(builder);
        keyNames.push(keyName);

        return builders.peek();
    }

    void endStructure(Deque<Object> builders, Deque<String> keyNames) {
        final Object completedStructure = builders.removeLast();

        if (builders.isEmpty()) {
            // Nothing to do, this is the top level builder
        } else {
            final String keyName = keyNames.pop();
            final JsonValue value = build(completedStructure);
            final Object parentBuilder = builders.peekLast();

            addValue(parentBuilder, keyName, value);
        }
    }

    JsonValue build(Object builder) {
        final JsonValue value;

        if (builder instanceof JsonObjectBuilder) {
            value = ((JsonObjectBuilder) builder).build();
        } else {
            value = ((JsonArrayBuilder) builder).build();
        }

        return value;
    }

    JsonValue getParsedValue(Event event) {
        JsonValue value;

        switch (event) {
        case VALUE_TRUE:
            value = JsonValue.TRUE;
            break;
        case VALUE_FALSE:
            value = JsonValue.FALSE;
            break;
        case VALUE_NULL:
            value = JsonValue.NULL;
            break;
        case VALUE_NUMBER:
            value = jsonProvider.createValue(getBigDecimal());
            break;
        case VALUE_STRING:
            value = jsonProvider.createValue(getString());
            break;
        default:
            throw new IllegalStateException("Non-value event: " + event);
        }

        return value;
    }

    void addValue(Object builder, String keyName, JsonValue value) {
        if (keyName != null) {
            ((JsonObjectBuilder) builder).add(keyName, value);
        } else {
            ((JsonArrayBuilder) builder).add(value);
        }
    }

    protected abstract M getMark(E event);
    protected abstract int getMarkLine(M mark);
    protected abstract int getMarkColumn(M mark);
    protected abstract int getMarkIndex(M mark);

    protected abstract String getAnchor(E event);

    protected abstract String getAlias(E event);

    protected abstract String getEventId(E event);

    protected abstract String getValue(E event);

    protected abstract boolean isPlain(E event);
}
