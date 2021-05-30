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
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.json.JsonException;
import jakarta.json.stream.JsonParsingException;

abstract class AbstractYamlParser<E, M> implements YamlParserCommon {

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

    private static final Logger LOGGER = Logger.getLogger(AbstractYamlParser.class.getName());

    static final String MSG_EXCEPTION = "Exception reading the YAML stream as JSON";
    static final String MSG_UNEXPECTED = "Unexpected event reached parsing YAML: ";

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

    final Reader yamlSource;
    final Iterator<E> yamlEvents;

    final Queue<E> yamlEventQueue = new ArrayDeque<>();
    final Queue<Event> jsonEventQueue = new ArrayDeque<>();
    final Queue<NumberType> numberTypeQueue = new ArrayDeque<>();
    final Queue<String> valueQueue = new ArrayDeque<>();

    final DecimalFormat decimalParser = new DecimalFormat();
    final ParsePosition decimalPosition = new ParsePosition(0);

    E currentYamlEvent;
    Event currentEvent;
    NumberType currentNumberType;
    String currentValue;
    BigDecimal currentNumber;

    final Boolean[] valueIsKey = new Boolean[200];
    int depth = -1;

    AbstractYamlParser(Iterator<E> yamlEvents, Reader yamlReader) {
        this.yamlEvents = yamlEvents;
        this.yamlSource = yamlReader;
    }

    void advanceEvent() {
        currentYamlEvent = yamlEventQueue.remove();
        currentEvent = jsonEventQueue.remove();
        currentNumberType = numberTypeQueue.remove();
        currentValue = valueQueue.remove();

        if (currentEvent == Event.VALUE_NUMBER) {
            this.currentNumber = parseNumber(currentNumberType, currentValue);
        }
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

    void enqueue(Event event, NumberType numberType, String value) {
        yamlEventQueue.add(currentYamlEvent);
        jsonEventQueue.add(event);
        numberTypeQueue.add(numberType != null ? numberType : NumberType.NONE);
        valueQueue.add(value != null ? value : "");
    }

    void enqueue(Event event) {
        this.enqueue(event, null, null);
    }

    void enqueue(Event event, String value) {
        this.enqueue(event, null, value);
    }

    void enqueueConstantOrString(String dataText, Set<String> constants, Event constantType) {
        final Event dataEvent = constants.contains(dataText) ? constantType : Event.VALUE_STRING;
        enqueue(dataEvent, dataText);
    }

    void enqueueNumberOrString(String dataText) {
        if (YamlNumbers.isInteger(dataText)) {
            enqueue(Event.VALUE_NUMBER, NumberType.INTEGER, dataText);
        } else if (YamlNumbers.isFloat(dataText)) {
            enqueue(Event.VALUE_NUMBER, NumberType.FLOAT, dataText);
        } else {
            enqueue(Event.VALUE_STRING, dataText);
        }
    }

    void enqueueZeroPrefixedValue(String dataText) {
        if (YamlNumbers.isOctal(dataText)) {
            enqueue(Event.VALUE_NUMBER, NumberType.OCTAL, dataText);
        } else if (YamlNumbers.isHexadecimal(dataText)) {
            enqueue(Event.VALUE_NUMBER, NumberType.HEXADECIMAL, dataText);
        } else {
            enqueueNumberOrString(dataText);
        }
    }

    void enqueueDotPrefixedValue(String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueue(Event.VALUE_STRING, NumberType.POSITIVE_INFINITY, dataText);
        } else if (VALUES_NAN.contains(dataText)) {
            enqueue(Event.VALUE_STRING, NumberType.NAN, dataText);
        } else if (YamlNumbers.isFloat(dataText)) {
            enqueue(Event.VALUE_NUMBER, NumberType.FLOAT, dataText);
        } else {
            enqueue(Event.VALUE_STRING, dataText);
        }
    }

    void enqueuePlusPrefixedValue(String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueue(Event.VALUE_STRING, NumberType.POSITIVE_INFINITY, dataText);
        } else {
            enqueueNumberOrString(dataText);
        }
    }

    void enqueueMinusPrefixedValue(String dataText) {
        if (VALUES_INFINITY.contains(dataText)) {
            enqueue(Event.VALUE_STRING, NumberType.NEGATIVE_INFINITY, dataText);
        } else {
            enqueueNumberOrString(dataText);
        }
    }

    void enqueueDataElement(final String dataText) {
        switch (dataText.charAt(0)) {
        case 'n':
        case 'N':
        case '~':
            enqueueConstantOrString(dataText, VALUES_NULL, Event.VALUE_NULL);
            break;

        case 't':
        case 'T':
            enqueueConstantOrString(dataText, VALUES_TRUE, Event.VALUE_TRUE);
            break;

        case 'f':
        case 'F':
            enqueueConstantOrString(dataText, VALUES_FALSE, Event.VALUE_FALSE);
            break;

        case '0':
            enqueueZeroPrefixedValue(dataText);
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
            enqueueNumberOrString(dataText);
            break;

        case '.':
            enqueueDotPrefixedValue(dataText);
            break;

        case '+':
            enqueuePlusPrefixedValue(dataText);
            break;

        case '-':
            enqueueMinusPrefixedValue(dataText);
            break;

        default:
            enqueue(Event.VALUE_STRING, dataText);
            break;
        }
    }

    void enqueueDataElement(E yamlEvent, Boolean needKeyName) {
        final String dataText = getValue(yamlEvent);

        if (Boolean.TRUE.equals(needKeyName)) {
            enqueue(Event.KEY_NAME, dataText);
        } else if (isPlain(yamlEvent)) {
            if (dataText.isEmpty()) {
                enqueue(Event.VALUE_NULL);
            } else {
                enqueueDataElement(dataText);
            }
        } else {
            enqueue(Event.VALUE_STRING, dataText);
        }
    }

    Boolean isKeyExpected() {
        return depth > -1 ? this.valueIsKey[depth] : null;
    }

    void incrementDepth(Boolean keyExpected) {
        if (isKeyExpected() != null) {
            this.valueIsKey[depth] = Boolean.TRUE;
        }

        depth++;
        this.valueIsKey[depth] = keyExpected;
    }

    boolean enqueueEvent(E yamlEvent) {
        LOGGER.finer(() -> "Enqueue YAML event: " + yamlEvent);
        currentYamlEvent = yamlEvent;
        currentNumber = null;
        boolean eventFound = true;
        String eventId = getEventId(yamlEvent);

        switch (eventId) {
        case "DocumentStart":
        case "DocumentEnd":
            eventFound = false;
            break;
        case "SequenceStart":
            incrementDepth(null);
            enqueue(Event.START_ARRAY);
            break;
        case "SequenceEnd":
            depth--;
            enqueue(Event.END_ARRAY);
            break;
        case "MappingStart":
            incrementDepth(Boolean.TRUE);
            enqueue(Event.START_OBJECT);
            break;
        case "MappingEnd":
            depth--;
            enqueue(Event.END_OBJECT);
            break;
        case "Scalar":
            Boolean keyExpected = isKeyExpected();
            enqueueDataElement(yamlEvent, keyExpected);
            if (keyExpected != null) {
                this.valueIsKey[depth] = Boolean.valueOf(!keyExpected);
            }
            break;
        case "Alias":
            // TODO Support for aliases
            eventFound = false;
            break;
        case "StreamStart":
        case "StreamEnd":
            eventFound = false;
            break;
        default:
            throw new IllegalStateException("Unknown state: " + eventId);
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
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();

            if (cause instanceof IOException) {
                throw new JsonException("IOException encountered reading YAML", cause);
            }

            throw new JsonParsingException("Exception reading YAML", re, this);
        } catch (Exception e) {
            throw new JsonParsingException("Exception reading YAML", e, this);
        }
    }

    void assertEventValueNumber() {
        final Event current = this.currentEvent;

        if (current != Event.VALUE_NUMBER) {
            throw new IllegalStateException("Unable to get number value for event [" + current + ']');
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
            throw new IllegalStateException("Unable to get string value for event [" + current + ']');
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
            throw new IllegalStateException("Current event is not a value [" + current + ']');
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
    public String getString() {
        assertEventValueString();
        return this.currentValue;
    }

    @Override
    public boolean isIntegralNumber() {
        assertEventValueNumber();
        return currentNumber.scale() == 0;
    }

    @Override
    public boolean isPositiveInfinity() {
        assertEventValue();
        return this.currentNumberType == NumberType.POSITIVE_INFINITY;
    }

    @Override
    public boolean isNegativeInfinity() {
        assertEventValue();
        return this.currentNumberType == NumberType.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isNaN() {
        assertEventValue();
        return this.currentNumberType == NumberType.NAN;
    }

    // JsonLocation

    @Override
    public long getLineNumber() {
        M mark = getMark();
        return mark != null ? getMarkLine(mark) + 1 : -1;
    }

    @Override
    public long getColumnNumber() {
        M mark = getMark();
        return mark != null ? getMarkColumn(mark) + 1 : -1;
    }

    @Override
    public long getStreamOffset() {
        M mark = getMark();
        return mark != null ? getMarkIndex(mark) : -1;
    }

    protected abstract M getMark();
    protected abstract int getMarkLine(M mark);
    protected abstract int getMarkColumn(M mark);
    protected abstract int getMarkIndex(M mark);

    protected abstract String getEventId(E event);

    protected abstract String getValue(E event);

    protected abstract boolean isPlain(E event);
}
