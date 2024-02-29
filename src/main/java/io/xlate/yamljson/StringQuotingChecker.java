package io.xlate.yamljson;

/**
 * Helper class originally copied from jackson-dataformat-yaml:
 * com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
 */
class StringQuotingChecker {

    private final boolean quoteNumericStrings;

    StringQuotingChecker(boolean quoteNumericStrings) {
        this.quoteNumericStrings = quoteNumericStrings;
    }

    /**
     * Check whether given property name should be quoted: usually to prevent it
     * from being read as non-String key (boolean or number)
     */
    boolean needToQuoteName(String name) {
        return needToQuote(name, true);
    }

    /**
     * Check whether given String value should be quoted: usually to prevent it
     * from being value of different type (boolean or number).
     */
    boolean needToQuoteValue(String value) {
        return needToQuote(value, quoteNumericStrings);
    }

    boolean needToQuote(String value, boolean quoteNumeric) {
        return value.isEmpty() ||
                isReservedKeyword(value) ||
                hasQuoteableCharacter(value) ||
                (quoteNumeric && YamlNumbers.isNumeric(value));
    }

    /**
     * See if given String value is one of
     * <ul>
     * <li>YAML 1.2 keyword representing boolean</li>
     * <li>YAML 1.2 keyword representing null value</li>
     *
     * @param value
     *            String to check
     *
     * @return {@code true} if given value is a Boolean or Null representation
     *         (as per YAML 1.2 specification) or empty String
     */
    boolean isReservedKeyword(String value) {
        switch (value.charAt(0)) {
        // First, reserved name starting chars:
        case 'f': // false
        case 'F': // False
            return YamlParser.VALUES_FALSE.contains(value);
        case 'n': // null
        case 'N': // Null
        case '~':
            return YamlParser.VALUES_NULL.contains(value);
        case 't': // true
        case 'T': // True
            return YamlParser.VALUES_TRUE.contains(value);
        default:
            return false;
        }
    }

    /**
     * As per YAML <a href="https://yaml.org/spec/1.2.2/#733-plain-style">Plain
     * Style</a>unquoted strings are restricted to a reduced charset and must be
     * quoted in case they contain one of the following characters or character
     * combinations.
     */
    boolean hasQuoteableCharacter(String inputStr) {
        if (quotableLeadingCharacter(inputStr)) {
            return true;
        }

        final int end = inputStr.length();

        for (int i = 1; i < end; ++i) {
            int current = inputStr.charAt(i);

            switch (current) {
            case '#':
                if (isBlank(inputStr.charAt(i - 1))) {
                    return true;
                }
                break;
            case ':':
                if (followedByBlank(inputStr, i)) {
                    return true;
                }
                break;
            default:
                if (current < 0x20) {
                    // Control character
                    return true;
                }
                break;
            }
        }

        // Check for trailing space
        return isBlank(inputStr.charAt(end - 1));
    }

    boolean quotableLeadingCharacter(String inputStr) {
        final int first = inputStr.charAt(0);

        switch (first) {
        case ' ':
            // Leading space
            return true;
        case '#':
        case ',':
        case '[':
        case ']':
        case '{':
        case '}':
        case '&':
        case '*':
        case '!':
        case '|':
        case '>':
        case '"':
        case '%':
        case '@':
        case '`':
            // Leading indicators
            return true;
        case '?':
        case ':':
        case '-':
            // Leading indicators not followed by non-space "safe" character
            if (followedByBlank(inputStr, 0)) {
                return true;
            }
            break;
        default:
            if (first < 0x20) {
                // Control character
                return true;
            }
            break;
        }

        return false;
    }

    boolean followedByBlank(String inputStr, int offset) {
        if (offset == inputStr.length() - 1) {
            return true;
        }
        return isBlank(inputStr.charAt(offset + 1));
    }

    boolean isBlank(char value) {
        return (' ' == value || '\t' == value);
    }
}
