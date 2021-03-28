package io.xlate.yamljson;

final class YamlNumbers {

    private YamlNumbers() {
    }

    static boolean isInteger(String dataText) {
        final int start;

        switch (dataText.charAt(0)) {
        case '-':
        case '+':
            start = 1;
            break;
        default:
            start = 0;
            break;
        }

        final int length = dataText.length();

        for (int i = start; i < length; i++) {
            switch (dataText.charAt(i)) {
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
                break;
            default:
                return false;
            }
        }

        return true;
    }

    static boolean isFloat(CharSequence value) {
        int length = value.length();

        int dec = 0;
        int exp = 0;
        boolean invalid = false;

        for (int i = 0, m = length; i < m && !invalid; i++) {
            switch (value.charAt(i)) {
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
                break;

            case 'e':
            case 'E':
                length--;

                if (++exp > 1) {
                    invalid = true;
                }
                break;

            case '+':
            case '-':
                length--;
                invalid = !validSign(i, value);
                break;

            case '.':
                length--;
                invalid = !validDecimalSymbol(++dec, exp);
                break;

            default:
                invalid = true;
                break;
            }
        }

        return !invalid;
    }

    static boolean validSign(int currentIndex, CharSequence value) {
        if (currentIndex == 0) {
            return true;
        }

        final char previousChar = value.charAt(currentIndex - 1);

        return previousChar == 'e' || previousChar == 'E';
    }

    static boolean validDecimalSymbol(int decimalCount, int exponentCount) {
        return !(decimalCount > 1 || exponentCount > 0);
    }

    static boolean isOctal(String dataText) {
        final int length = dataText.length();

        if (length < 3 || !dataText.startsWith("0o")) {
            return false;
        }

        for (int i = 2; i < length; i++) {
            switch (dataText.charAt(i)) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                break;
            default:
                return false;
            }
        }

        return true;
    }

    static boolean isHexadecimal(String dataText) {
        final int length = dataText.length();

        if (length < 3 || !dataText.startsWith("0x")) {
            return false;
        }

        for (int i = 2; i < length; i++) {
            switch (dataText.charAt(i)) {
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
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                break;
            default:
                return false;
            }
        }

        return true;
    }

}
