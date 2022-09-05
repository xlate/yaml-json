package io.xlate.yamljson;

import jakarta.json.stream.JsonLocation;

class YamlLocation implements JsonLocation {

    final long lineNumber;
    final long columnNumber;
    final long streamOffset;

    public YamlLocation(long lineNumber, long columnNumber, long streamOffset) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.streamOffset = streamOffset;
    }

    @Override
    public long getLineNumber() {
        return lineNumber;
    }

    @Override
    public long getColumnNumber() {
        return columnNumber;
    }

    @Override
    public long getStreamOffset() {
        return streamOffset;
    }

}
