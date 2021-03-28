package io.xlate.yamljson;

import java.util.Set;

/**
 * Helper class originally copied from jackson-dataformat-yaml:
 * com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
 */
class StringQuotingChecker {

    StringQuotingChecker() {
    }

    /**
     * As per YAML <a href="https://yaml.org/type/null.html">null</a> and
     * <a href="https://yaml.org/type/bool.html">boolean</a> type specs, better
     * retain quoting for some keys (property names) and values.
     */
    // @formatter:off
    private static final Set<String> RESERVED_KEYWORDS = Set.of(
        "false", "False", "FALSE",
        "n", "N", "no", "No", "NO",
        "null", "Null", "NULL",
        "on", "On", "ON",
        "off", "Off", "OFF",
        "true", "True", "TRUE",
        "y", "Y", "yes", "Yes", "YES");
    // @formatter:on

    /**
     * Check whether given property name should be quoted: usually to prevent it
     * from being read as non-String key (boolean or number)
     */
    boolean needToQuoteName(String name) {
        return isReservedKeyword(name) || looksLikeYAMLNumber(name);
    }

    /**
     * Check whether given String value should be quoted: usually to prevent it
     * from being value of different type (boolean or number).
     */
    boolean needToQuoteValue(String value) {
        // Only consider reserved keywords but not numbers?
        return isReservedKeyword(value) || valueHasQuotableChar(value);
    }

    /**
     * See if given String value is one of:
     * <ul>
     * <li>YAML 1.1 keyword representing
     * <a href="https://yaml.org/type/bool.html">boolean</a></li>
     * <li>YAML 1.1 keyword representing
     * <a href="https://yaml.org/type/null.html">null</a> value</li>
     * <li>empty String (length 0)</li></li> and returns {@code true} if so.
     *
     * @param value
     *            String to check
     *
     * @return {@code true} if given value is a Boolean or Null representation
     *         (as per YAML 1.1 specification) or empty String
     */
    boolean isReservedKeyword(String value) {
        if (value.length() == 0) {
            return true;
        }

        switch (value.charAt(0)) {
        // First, reserved name starting chars:
        case 'f': // false
        case 'n': // no/n/null
        case 'o': // on/off
        case 't': // true
        case 'y': // yes/y
        case 'F': // False
        case 'N': // No/N/Null
        case 'O': // On/Off
        case 'T': // True
        case 'Y': // Yes/Y
            return RESERVED_KEYWORDS.contains(value);
        default:
            return false;
        }
    }

    /**
     * See if given String value looks like a YAML 1.1 numeric value and would
     * likely be considered a number when parsing unless quoting is used.
     */
    boolean looksLikeYAMLNumber(String name) {
        if (name.length() > 0) {
            switch (name.charAt(0)) {
            // And then numbers
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '+':
            case '.':
                return true;
            default:
                break;
            }
        }

        return false;
    }

    /**
     * As per YAML <a href="https://yaml.org/spec/1.2/spec.html#id2788859">Plain
     * Style</a>unquoted strings are restricted to a reduced charset and must be
     * quoted in case they contain one of the following characters or character
     * combinations.
     */
    boolean valueHasQuotableChar(String inputStr) {
        final int end = inputStr.length();
        for (int i = 0; i < end; ++i) {
            switch (inputStr.charAt(i)) {
            case '[':
            case ']':
            case '{':
            case '}':
            case ',':
                return true;
            case '#':
                // [dataformats-text#201]: limit quoting with MINIMIZE_QUOTES
                if (precededByBlank(inputStr, i)) {
                    return true;
                }
                break;
            case ':':
                // [dataformats-text#201]: limit quoting with MINIMIZE_QUOTES
                if (followedByBlank(inputStr, i)) {
                    return true;
                }
                break;
            default:
                break;
            }
        }
        return false;
    }

    boolean precededByBlank(String inputStr, int offset) {
        if (offset == 0) {
            return false;
        }
        return isBlank(inputStr.charAt(offset - 1));
    }

    boolean followedByBlank(String inputStr, int offset) {
        if (offset == inputStr.length() - 1) {
            return false;
        }
        return isBlank(inputStr.charAt(offset + 1));
    }

    boolean isBlank(char value) {
        return (' ' == value || '\t' == value);
    }
}
