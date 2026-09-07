package cz.mcsworld.eroded.config;

/**
 * Tiny strict RFC-8259 style JSON syntax validator.
 *
 * <p>Gson's streaming reader can operate leniently and therefore accepts
 * tokens such as {@code tru}, {@code fajse} or other unquoted words. When a
 * boolean field is deserialized, those strings can silently become false.
 * Eroded World config files must be real JSON, so validation happens before
 * the previous config library the config loader/Gson is allowed to deserialize them.</p>
 */
public final class StrictJsonSyntax {

    private StrictJsonSyntax() {
    }

    public static void validate(String json) {
        if (json == null) {
            throw new SyntaxException("prázdný vstup", 1, 1);
        }

        Parser parser = new Parser(json);
        parser.skipWhitespace();
        parser.parseValue();
        parser.skipWhitespace();

        if (!parser.atEnd()) {
            parser.fail("neočekávaná data za koncem JSON hodnoty");
        }
    }

    public static final class SyntaxException extends IllegalArgumentException {
        private final int line;
        private final int column;

        private SyntaxException(String message, int line, int column) {
            super(message + " (řádek " + line + ", sloupec " + column + ")");
            this.line = line;
            this.column = column;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }
    }

    private static final class Parser {
        private final String input;
        private int index;
        private int line = 1;
        private int column = 1;

        private Parser(String input) {
            // UTF-8 BOM is tolerated only as the very first character.
            this.input = !input.isEmpty() && input.charAt(0) == '\uFEFF'
                    ? input.substring(1)
                    : input;
        }

        private boolean atEnd() {
            return index >= input.length();
        }

        private char peek() {
            if (atEnd()) {
                fail("neočekávaný konec souboru");
            }
            return input.charAt(index);
        }

        private char consume() {
            char c = peek();
            index++;
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            return c;
        }

        private void skipWhitespace() {
            while (!atEnd()) {
                char c = input.charAt(index);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    consume();
                } else {
                    break;
                }
            }
        }

        private void parseValue() {
            skipWhitespace();
            if (atEnd()) {
                fail("očekávána JSON hodnota");
            }

            switch (peek()) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true");
                case 'f' -> parseLiteral("false");
                case 'n' -> parseLiteral("null");
                default -> {
                    char c = peek();
                    if (c == '-' || isDigit(c)) {
                        parseNumber();
                    } else {
                        fail("neplatný token; řetězce musí být v uvozovkách a boolean musí být true/false");
                    }
                }
            }
        }

        private void parseObject() {
            expect('{');
            skipWhitespace();
            if (tryConsume('}')) {
                return;
            }

            while (true) {
                skipWhitespace();
                if (atEnd() || peek() != '"') {
                    fail("očekáván název položky v uvozovkách");
                }
                parseString();
                skipWhitespace();
                expect(':');
                parseValue();
                skipWhitespace();

                if (tryConsume('}')) {
                    return;
                }
                expect(',');
                skipWhitespace();
                if (!atEnd() && peek() == '}') {
                    fail("čárka za poslední položkou objektu není platný JSON");
                }
            }
        }

        private void parseArray() {
            expect('[');
            skipWhitespace();
            if (tryConsume(']')) {
                return;
            }

            while (true) {
                parseValue();
                skipWhitespace();
                if (tryConsume(']')) {
                    return;
                }
                expect(',');
                skipWhitespace();
                if (!atEnd() && peek() == ']') {
                    fail("čárka za poslední položkou pole není platný JSON");
                }
            }
        }

        private void parseString() {
            expect('"');
            while (!atEnd()) {
                char c = consume();
                if (c == '"') {
                    return;
                }
                if (c == '\\') {
                    if (atEnd()) {
                        fail("nedokončená escape sekvence v řetězci");
                    }
                    char escaped = consume();
                    switch (escaped) {
                        case '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                        }
                        case 'u' -> parseUnicodeEscape();
                        default -> fail("neplatná escape sekvence \\" + escaped + " v řetězci");
                    }
                    continue;
                }
                if (c <= 0x1F) {
                    fail("řídicí znak musí být v JSON řetězci escapovaný");
                }
            }
            fail("neukončený řetězec");
        }

        private void parseUnicodeEscape() {
            for (int i = 0; i < 4; i++) {
                if (atEnd() || !isHex(peek())) {
                    fail("\\u escape musí obsahovat přesně 4 hexadecimální číslice");
                }
                consume();
            }
        }

        private void parseLiteral(String literal) {
            for (int i = 0; i < literal.length(); i++) {
                if (atEnd() || consume() != literal.charAt(i)) {
                    fail("neplatný literál; povoleno je pouze true, false nebo null");
                }
            }

            if (!atEnd()) {
                char next = input.charAt(index);
                if (Character.isLetterOrDigit(next) || next == '_' || next == '-') {
                    fail("neplatný literál; povoleno je pouze true, false nebo null");
                }
            }
        }

        private void parseNumber() {
            if (tryConsume('-') && atEnd()) {
                fail("neplatné číslo");
            }

            if (tryConsume('0')) {
                if (!atEnd() && isDigit(input.charAt(index))) {
                    fail("číslo nesmí mít úvodní nulu");
                }
            } else {
                if (atEnd() || !isDigit19(peek())) {
                    fail("neplatné číslo");
                }
                while (!atEnd() && isDigit(input.charAt(index))) {
                    consume();
                }
            }

            if (tryConsume('.')) {
                if (atEnd() || !isDigit(peek())) {
                    fail("za desetinnou tečkou musí být číslice");
                }
                while (!atEnd() && isDigit(input.charAt(index))) {
                    consume();
                }
            }

            if (!atEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                consume();
                if (!atEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                    consume();
                }
                if (atEnd() || !isDigit(peek())) {
                    fail("exponent musí obsahovat číslice");
                }
                while (!atEnd() && isDigit(input.charAt(index))) {
                    consume();
                }
            }
        }

        private boolean tryConsume(char expected) {
            if (!atEnd() && input.charAt(index) == expected) {
                consume();
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (atEnd() || peek() != expected) {
                fail("očekáván znak '" + expected + "'");
            }
            consume();
        }

        private void fail(String message) {
            throw new SyntaxException(message, line, column);
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static boolean isDigit19(char c) {
            return c >= '1' && c <= '9';
        }

        private static boolean isHex(char c) {
            return (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
        }
    }
}
