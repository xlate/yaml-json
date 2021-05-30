package io.xlate.yamljson;

/**
 * Helper class originally copied from jackson-dataformat-yaml:
 * com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
 */
class StringQuotingChecker {

    StringQuotingChecker() {
    }

    /**
     * Check whether given property name should be quoted: usually to prevent it
     * from being read as non-String key (boolean or number)
     */
    boolean needToQuoteName(String name) {
        return isReservedKeyword(name) || YamlNumbers.isFloat(name);
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
     * See if given String value is one of
     * <ul>
     * <li>YAML 1.2 keyword representing boolean</li>
     * <li>YAML 1.2 keyword representing null value</li>
     * <li>empty String (length 0)</li></li> and returns {@code true} if so.
     *
     * @param value
     *            String to check
     *
     * @return {@code true} if given value is a Boolean or Null representation
     *         (as per YAML 1.2 specification) or empty String
     */
    boolean isReservedKeyword(String value) {
        if (value.length() == 0) {
            return true;
        }

        switch (value.charAt(0)) {
        // First, reserved name starting chars:
        case 'f': // false
        case 'F': // False
            return AbstractYamlParser.VALUES_FALSE.contains(value);
        case 'n': // null
        case 'N': // Null
        case '~':
            return AbstractYamlParser.VALUES_NULL.contains(value);
        case 't': // true
        case 'T': // True
            return AbstractYamlParser.VALUES_TRUE.contains(value);
        default:
            return false;
        }
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
                if (precededByBlank(inputStr, i)) {
                    return true;
                }
                break;
            case ':':
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
            return true;
        }
        return isBlank(inputStr.charAt(offset - 1));
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
