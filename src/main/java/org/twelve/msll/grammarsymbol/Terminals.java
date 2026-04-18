package org.twelve.msll.grammarsymbol;

import org.twelve.msll.exception.LexerException;
import org.twelve.msll.lexer.Line;
import org.twelve.msll.lexer.Location;
import org.twelve.msll.lexer.MatchResult;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.RegexString;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Stores all terminal symbols for a specific parser.
 * Each terminal includes metadata for token matching, which can either be a full match string or a regex pattern.
 * <p>
 * When matching a token, three principles apply in case of multiple matches:
 * 1. **Maximal Munch**: The longest matching token is selected.
 * 2. **Keyword Priority**: If multiple matches exist, string definitions (keywords) have a higher priority than regex matches.
 * 3. **Order of Definition**: If matches are equivalent by length and priority, the last defined match is selected.
 *
 * @author huizi 2024
 */
public class Terminals implements SymbolTypes<Terminal> {
    protected final List<Terminal> terminals = new ArrayList<>();

    /**
     * Flat array of [WHITESPACE, ...this.terminals] cached for the match() hot path.
     * Re-built lazily whenever terminals change.  Avoids O(N_lines) ArrayList creation
     * and copy overhead inside match().
     */
    private Terminal[] cachedTerminalArray = null;

    /**
     * Per-mode cached terminal arrays.  Built lazily on first access for each mode name.
     * Cleared whenever terminals change (same lifecycle as cachedTerminalArray).
     */
    private final Map<String, Terminal[]> modeTerminalCache = new HashMap<>();
    public final Terminal END;
    public final Terminal EOL;
    public final Terminal OR_OR;
    public final Terminal OR;
    public final Terminal COLON;
    public final Terminal SEMICOLON;
    public final Terminal EPSILON;

    // Constructor to initialize commonly used terminals for parsing
    public Terminals() {
        this.addTerminal(Constants.LEFT_PAREN_STR, Constants.LEFT_PAREN);
        this.addTerminal(Constants.RIGHT_PAREN_STR, Constants.RIGHT_PAREN);
        this.addTerminal(Constants.QUESTION_STR, Constants.QUESTION);
        this.addTerminal(Constants.STAR_STR, Constants.STAR);
        END = this.addTerminal(Constants.END_STR, new RegexString(Constants.END));
        EOL = this.addTerminal(Constants.EOL_STR, new RegexString(Constants.EOL));
        OR_OR = this.addTerminal(Constants.OR_OR_STR, Constants.OR_OR);
        OR = this.addTerminal(Constants.OR_STR, Constants.OR);
        COLON = this.addTerminal(Constants.COLON_STR, Constants.COLON);
        SEMICOLON = this.addTerminal(Constants.SEMICOLON_STR, Constants.SEMICOLON);
        EPSILON = this.addIfAbsent(Terminal.EPSILON);
    }

    private static Terminals parserTerminals = null;
    private static Terminals lexerTerminals = null;
    private static Terminals myTerminals = null;

    /**
     * Method to get terminals for MyParser
     */
    public synchronized static Terminals my() {
        if (myTerminals == null) {
            myTerminals = buildMyTerminals();
        }
        return myTerminals;
    }

    /**
     * Returns a <em>fresh</em> {@code Terminals} instance seeded with the same
     * built-in operator / punctuation terminals as {@link #my()}. Unlike
     * {@link #my()}, no state is shared across calls &mdash; necessary when
     * multiple user grammars are built in the same JVM (e.g. the grammars-v4
     * compatibility harness), since the user grammar builder mutates its
     * {@code Terminals} collection by registering every lexer rule it sees.
     * Reusing the singleton makes rules from grammar A leak into grammar B's
     * predict table.
     */
    public static Terminals newMy() {
        return buildMyTerminals();
    }

    /**
     * Returns a <em>fresh</em>, <em>minimal</em> {@code Terminals} instance
     * containing only the structural built-ins seeded by the constructor
     * ({@code (}, {@code )}, {@code ?}, {@code *}, {@code |}, {@code :},
     * {@code ;}, {@code END}, {@code EOL}, {@code EPSILON}). It does
     * <em>not</em> add any Outline-language tokens (no {@code STRING},
     * {@code ++}, {@code ==}, {@code COMMA}, {@code DOT}, etc.).
     *
     * <p>This is the seed used by the G4 loading path so that built-in
     * Outline tokens do not pollute a user grammar's terminal table and
     * compete with terminals declared in the user's own {@code .g4}
     * (e.g. focal's {@code STRING_LITERAL} losing to MSLL's built-in
     * {@code STRING}). Any token a G4 grammar needs must come from the
     * grammar itself.
     */
    public static Terminals newBare() {
        Terminals bare = new Terminals();
        // The base constructor also seeds a handful of punctuation tokens
        // (LEFT_PAREN, RIGHT_PAREN, QUESTION, STAR, OR, OR_OR, COLON,
        // SEMICOLON) because the .gm meta-grammar parser itself consumes
        // them. In a user grammar loaded via G4 those characters are
        // whatever the user declares they are — COLON out-competing a
        // user-declared `SEP : [=:]` token is exactly the kind of
        // cross-contamination the bare seed is meant to prevent. Strip
        // them here; keep only EOL / END / EPSILON, which the MSLL
        // runtime itself inserts as sentinels during lexing.
        bare.terminals.removeIf(t -> {
            String n = t.name();
            return n.equals(Constants.LEFT_PAREN_STR)
                || n.equals(Constants.RIGHT_PAREN_STR)
                || n.equals(Constants.QUESTION_STR)
                || n.equals(Constants.STAR_STR)
                || n.equals(Constants.OR_OR_STR)
                || n.equals(Constants.OR_STR)
                || n.equals(Constants.COLON_STR)
                || n.equals(Constants.SEMICOLON_STR);
        });
        bare.cachedTerminalArray = null;
        bare.modeTerminalCache.clear();
        return bare;
    }

    private static Terminals buildMyTerminals() {
        Terminals myTerminals = new Terminals();
            // Support escape sequences (e.g. \n, \t, \") inside string literals
            myTerminals.addTerminal(Constants.STRING, new RegexString("(\\\"(?:[^\\\"\\\\]|\\\\.)*\\\")"));
            myTerminals.addTerminal(Constants.PLUS_PLUS_STR, Constants.PLUS_PLUS);
            myTerminals.addTerminal(Constants.MINUS_MINUS_STR, Constants.MINUS_MINUS);
            myTerminals.addTerminal(Constants.BANG_EQUAL_STR, Constants.BANG_EQUAL);
            myTerminals.addTerminal(Constants.EQUAL_EQUAL_STR, Constants.EQUAL_EQUAL);
            myTerminals.addTerminal(Constants.GREATER_EQUAL_STR, Constants.GREATER_EQUAL);
            myTerminals.addTerminal(Constants.LESS_EQUAL_STR, Constants.LESS_EQUAL);
            myTerminals.addTerminal(Constants.AND_AND_STR, Constants.AND_AND);
            myTerminals.addTerminal(Constants.PLUS_EQUAL_STR, Constants.PLUS_EQUAL);
            myTerminals.addTerminal(Constants.MINUS_EQUAL_STR, Constants.MINUS_EQUAL);
            myTerminals.addTerminal(Constants.STAR_EQUAL_STR, Constants.STR_EQUAL);
            myTerminals.addTerminal(Constants.SLASH_EQUAL_STR, Constants.SLASH_EQUAL);
            myTerminals.addTerminal(Constants.WAVE_STR, Constants.WAVE);

            myTerminals.addTerminal(Constants.LEFT_BRACE_STR, Constants.LEFT_BRACE);
            myTerminals.addTerminal(Constants.RIGHT_BRACE_STR, Constants.RIGHT_BRACE);
            myTerminals.addTerminal(Constants.LEFT_BRACKET_STR, Constants.LEFT_BRACKET);
            myTerminals.addTerminal(Constants.RIGHT_BRACKET_STR, Constants.RIGHT_BRACKET);
            myTerminals.addTerminal(Constants.COMMA_STR, Constants.COMMA);
            myTerminals.addTerminal(Constants.DOT_STR, Constants.DOT);
            myTerminals.addTerminal(Constants.PLUS_STR, Constants.PLUS);
            myTerminals.addTerminal(Constants.MINUS_STR, Constants.MINUS);
            myTerminals.addTerminal(Constants.BANG_STR, Constants.BANG);
            myTerminals.addTerminal(Constants.EQUAL_STR, Constants.EQUAL);
            myTerminals.addTerminal(Constants.GREATER_STR, Constants.GREATER);
            myTerminals.addTerminal(Constants.UNDER_LINE_STR, Constants.UNDER_LINE);
            myTerminals.addTerminal(Constants.LESS_STR, Constants.LESS);
            myTerminals.addTerminal(Constants.SLASH_STR, Constants.SLASH);
            myTerminals.addTerminal(Constants.AND_STR, Constants.AND);
            myTerminals.addTerminal(Constants.MOD_STR, Constants.MOD);
            myTerminals.addTerminal(Constants.POWER_STR, Constants.POWER);

            myTerminals.addTerminal(Constants.ENTER, new RegexString("\\S* \\n"));
            myTerminals.addTerminal(Constants.DOUBLE, new RegexString("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?!\\.\\d)[d](?!\\w)"));
            myTerminals.addTerminal(Constants.FLOAT, new RegexString("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?!\\.\\d)[f](?!\\w)"));
        return myTerminals;
    }

    /**
     * Method to get terminals for parser grammar parsing
     */
    public synchronized static Terminals parser() {
        if (parserTerminals == null) {
            parserTerminals = new Terminals();
            parserTerminals.addTerminal(Constants.PARSER_GRAMMAR, new RegexString("^parser\\s+grammar\\b"));
            // Allow the opposite-quote type inside: 'can contain "' and "can contain '"
            parserTerminals.addTerminal(Constants.STRING, new RegexString("'[^']*'|\"[^\"]*\""));

            fillTerminals(parserTerminals);
            parserTerminals.addTerminal(Constants.ID, new RegexString("([a-z_][\\w,']*)"));
            parserTerminals.addTerminal(Constants.UPPER_ID, new RegexString("(\\b[A-Z]\\w*)"));
        }
        return parserTerminals;
    }

    /**
     * Method to get terminals for lexer grammar parsing
     */
    public synchronized static Terminals lexer() {
        if (lexerTerminals == null) {
            lexerTerminals = new Terminals();
            // Allow the opposite-quote type inside: 'can contain "' and "can contain '"
            lexerTerminals.addTerminal(Constants.STRING, new RegexString("'[^']*'|\"[^\"]*\""));
            lexerTerminals.addTerminal(Constants.LEXER_GRAMMAR, new RegexString("^lexer\\s+grammar\\b"));
            lexerTerminals.addTerminal(Constants.ANY, Constants.DOT);
            lexerTerminals.addTerminal(Constants.SPECIAL, new RegexString("\\\\(d|D|b|B|S|s|W|w|\\*|\\?|\\+|\\||\\\\)"));
            lexerTerminals.addTerminal(Constants.LEFT_BRACKET_STR, Constants.LEFT_BRACKET);
            lexerTerminals.addTerminal(Constants.RIGHT_BRACKET_STR, Constants.RIGHT_BRACKET);
            lexerTerminals.addTerminal(Constants.SINGLE_CHARACTER, new RegexString("\\[\\^?(?:\\\\.|[^\\]])+\\]"));
            // NOT (~) operator for negated char classes: ~[...] or ~'x'
            lexerTerminals.addTerminal(Constants.NOT_STR, Constants.NOT);

            fillTerminals(lexerTerminals);
            lexerTerminals.addTerminal(Constants.ID, new RegexString("([a-z][A-Za-z0-9_]*)"));
            lexerTerminals.addTerminal(Constants.UPPER_ID, new RegexString("([A-Z][A-Za-z0-9_]*)"));
        }
        return lexerTerminals;
    }

    /**
     * Method to add commonly used terminal symbols
     */
    private static void fillTerminals(Terminals terminals) {
        terminals.addTerminal(Constants.REGEX, new RegexString("/\\\".+\\\"/"));
        terminals.addTerminal(Constants.FRAGMENT.toUpperCase(), Constants.FRAGMENT);
        terminals.addTerminal(Constants.MODE_STR, Constants.MODE);
        // PREDICATE matches {action code} or {predicate}? bodies.
        // Excludes commas (channels/options separators) and newlines (multi-line blocks).
        terminals.addTerminal(Constants.PREDICATE, new RegexString("\\{[^},\\n\\r]*\\}"));
        terminals.addTerminal(Constants.EXPLAIN, new RegexString("#\\s*[\\w,\\s]*"));
        terminals.addTerminal(Constants.PLUS_STR, Constants.PLUS);
        terminals.addTerminal(Constants.LEFT.toUpperCase(), Constants.LEFT);
        terminals.addTerminal(Constants.RIGHT.toUpperCase(), Constants.RIGHT);
        terminals.addTerminal(Constants.NONE.toUpperCase(), Constants.NONE);
        terminals.addTerminal(Constants.LONG_COMMENT, new RegexString("\\/\\*[\\s\\S]*?\\*\\/"));
        terminals.addTerminal(Constants.COMMENT, new RegexString("\\/\\/.*"));
        terminals.addTerminal(Constants.OPTIONS_STR, Constants.OPTIONS);
        terminals.addTerminal(Constants.LEFT_BRACE_STR, Constants.LEFT_BRACE);
        terminals.addTerminal(Constants.RIGHT_BRACE_STR, Constants.RIGHT_BRACE);
        terminals.addTerminal(Constants.CHANNELS.toUpperCase(), Constants.CHANNELS);
        terminals.addTerminal(Constants.LEXER_COMMAND, new RegexString("->[\\w\\(\\)\\,\\s]*"));
        terminals.addTerminal(Constants.EQUAL_STR, Constants.EQUAL);
        terminals.addTerminal(Constants.COMMA_STR, Constants.COMMA);
        terminals.addTerminal(Constants.LESS_STR, Constants.LESS);
        terminals.addTerminal(Constants.ASSOC.toUpperCase(), Constants.ASSOC);
        terminals.addTerminal(Constants.GREATER_STR, Constants.GREATER);
        terminals.addTerminal(Constants.UNDER_LINE_STR, Constants.UNDER_LINE);
    }

    /**
     * Matches the given input string to the terminal rules and returns the list of matched tokens.
     *
     * @param line      The input line to be tokenized.
     * @param lineNum   The current line number.
     * @param charIndex The current character index.
     * @return A list of matched tokens.
     * @throws LexerException if an unexpected character is found.
     */
    public List<Token> match(String line, int lineNum, int charIndex) throws LexerException {
        // P2: use a per-Terminals cached flat array instead of building a new ArrayList per call.
        // During parsing the terminal set is frozen, so the cache is always valid here.
        Terminal[] allTerminals = this.cachedTerminalArray;
        if (allTerminals == null) {
            allTerminals = new Terminal[this.terminals.size() + 1];
            allTerminals[0] = Terminal.WHITESPACE;
            for (int i = 0; i < this.terminals.size(); i++) {
                allTerminals[i + 1] = this.terminals.get(i);
            }
            this.cachedTerminalArray = allTerminals;
        }
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        int length = line.length();

        while (true) {
            String remainingInput = line.substring(position);
            Token bestMatch = null;
            int maxMatchLength = -1;

            // P1: use pre-compiled Pattern cached on Terminal; only Matcher is created here.
            for (Terminal terminal : allTerminals) {
                Matcher matcher = terminal.compiledPattern().matcher(remainingInput);
                if (matcher.lookingAt()) {
                    String name = terminal.tokenName();
                    String value = matcher.group(name);
                    int matchLength = value.length();
                    if (matchLength > maxMatchLength || (matchLength == maxMatchLength && !terminal.isRegex())) {
                        maxMatchLength = matchLength;
                        bestMatch = new Token(terminal, value, new Location(matcher.start(name) + position, matcher.end(name) + position, new Line(lineNum, charIndex)));
                    }
                    // If same length, prioritize the first defined terminal
                    else if (matchLength == maxMatchLength && bestMatch != null) {
                        // Assuming terminals are ordered by priority
                        // Do nothing, as bestMatch is already the first terminal with this length
                    }
                }
            }

            if (bestMatch != null) {
                if (!bestMatch.terminal().name().equals(Constants.WHITESPACE_STR)) {
                    tokens.add(bestMatch);
                }
                position = bestMatch.location().lineEnd();
                if (bestMatch.terminal().name.equals(Constants.EOL_STR) && position == length) break;
            } else {
                // Build a clear, positioned error message
                char badChar = line.charAt(position);
                String before = line.substring(0, position);
                String pointer = " ".repeat(position) + "^";
                throw new LexerException(
                        "Unexpected character '" + badChar + "'"
                        + " at line " + lineNum + ", column " + position
                        + System.lineSeparator()
                        + "  " + line
                        + System.lineSeparator()
                        + "  " + pointer
                );
            }
        }

        return tokens;
    }

    @Override
    public Terminal fromName(String lexeme) {
        Optional<Terminal> terminal = this.terminals.stream().filter(t -> !t.name().isEmpty() && (t.name().equals(lexeme.trim()) || t.pattern().equals(lexeme.replace("\"", "").trim()))).findFirst();
        if (terminal.isPresent()) {
            return terminal.get();
        } else {
            return null;
        }
    }

    /**
     * Finds a user-defined terminal whose compiled pattern fully matches the
     * given literal lexeme. Used by {@link org.twelve.msll.lexer.RegexLexer}
     * to decide whether to synthesise a newline token on line boundaries when
     * the user grammar declares {@code '\n'}, {@code '\r\n'}, or {@code '\r'}
     * as an explicit token (e.g. CSV / properties / INI style grammars).
     *
     * <p>The built-in {@code WHITESPACE}, {@code EPSILON} and {@code EOL}
     * terminals are skipped so we do not accidentally return a sentinel.
     *
     * @param lexeme literal string to probe (e.g. {@code "\n"})
     * @return matching user terminal, or {@code null} if none declared
     */
    public Terminal findTerminalForLexeme(String lexeme) {
        if (lexeme == null || lexeme.isEmpty()) return null;
        for (Terminal t : this.terminals) {
            String n = t.name();
            if (n.equals(Constants.WHITESPACE_STR)
                    || n.equals(Constants.EPSILON_STR)
                    || n.equals(Constants.EOL_STR)
                    || n.equals(Constants.END_STR)) continue;
            Matcher m = t.compiledPattern().matcher(lexeme);
            if (m.matches()) {
                return t;
            }
        }
        return null;
    }

    public Terminal fromPattern(String pattern) {
        if (pattern == null || pattern.trim() == Constants.EMPTY) return null;
        Optional<Terminal> terminal = this.terminals.stream().filter(t -> t.pattern().equals(pattern.trim()) || t.pattern().equals(pattern.replace("\"", "").trim())).findFirst();
        if (terminal.isPresent()) {
            return terminal.get();
        } else {
            return null;
        }
    }

    @Override
    public Terminal addSymbol(Terminal symbolType) {
        Terminal old = this.fromName(symbolType.name());
        if (old == null) {
            // Only dedup by pattern when the new terminal lives in the same
            // lexer mode as an existing one. With lexer modes it is legitimate
            // for two differently-named rules in different modes to share a
            // pattern (e.g. DEFAULT's `NL : '\n'` and VAL's
            // `NL_VAL : '\n' -> type(NL), popMode`). Collapsing them loses
            // the mode-specific rule and its lexer command.
            Terminal byPattern = this.fromPattern(symbolType.pattern());
            if (byPattern != null && java.util.Objects.equals(byPattern.mode(), symbolType.mode())) {
                old = byPattern;
            }
        }
        if (old == null) {
            old = symbolType;
            this.terminals.add(symbolType);
        } else {
            old.refresh(symbolType);
            this.terminals.remove(old);
            this.terminals.removeIf(t -> t.pattern().equals(symbolType.pattern()));
            this.terminals.add(old);
        }
        this.cachedTerminalArray = null;
        this.modeTerminalCache.clear();
        return old;
    }

    public Terminal addTerminal(String name, String pattern) {
        Terminal terminal = new Terminal(name, pattern);
        this.terminals.add(terminal);
        this.cachedTerminalArray = null;
        this.modeTerminalCache.clear();
        return terminal;
    }

    public Terminal addTerminal(String name, RegexString rStr) {
        Terminal terminal = new Terminal(name, rStr);
        this.terminals.add(terminal);
        this.cachedTerminalArray = null;
        this.modeTerminalCache.clear();
        return terminal;
    }

    @Override
    public List<Terminal> values() {
        return this.terminals;
    }

    @Override
    public Terminal addIfAbsent(Terminal symbolType) {
        Terminal old = this.fromName(symbolType.name());
        if (old == null) {
            old = this.fromPattern(symbolType.pattern());
        }
        if (old == null) {
            old = symbolType;
            this.terminals.add(0, symbolType);
            this.cachedTerminalArray = null;
            this.modeTerminalCache.clear();
        }
        return old;
    }

    // -----------------------------------------------------------------------
    // Mode-aware matching
    // -----------------------------------------------------------------------

    /**
     * Returns the terminal array to use for the given mode.
     *
     * <ul>
     *   <li>If no terminal has an explicit mode set (plain grammar files without mode
     *       sections), all terminals are returned — backward-compatible behaviour.</li>
     *   <li>Otherwise only terminals belonging to {@code mode} (or with {@code mode==null},
     *       i.e. built-ins) are returned.</li>
     * </ul>
     */
    private Terminal[] getActiveTerminalsFor(String mode) {
        boolean hasModeSpecific = this.terminals.stream()
                .anyMatch(t -> t.mode() != null && !t.mode().equals("DEFAULT_MODE"));
        if (!hasModeSpecific) {
            // No mode sections → use the global cached array (backward compat)
            if (this.cachedTerminalArray == null) {
                Terminal[] arr = new Terminal[this.terminals.size() + 1];
                arr[0] = Terminal.WHITESPACE;
                for (int i = 0; i < this.terminals.size(); i++) arr[i + 1] = this.terminals.get(i);
                this.cachedTerminalArray = arr;
            }
            return this.cachedTerminalArray;
        }
        String key = mode == null ? "DEFAULT_MODE" : mode;
        return modeTerminalCache.computeIfAbsent(key, m -> {
            List<Terminal> active = new ArrayList<>();
            active.add(Terminal.WHITESPACE);
            for (Terminal t : this.terminals) {
                // null mode = always active (built-in); matching mode = active
                if (t.mode() == null || t.mode().equals(m)) {
                    active.add(t);
                }
            }
            return active.toArray(new Terminal[0]);
        });
    }

    /**
     * Matches the <em>next single token</em> starting at the beginning of {@code remaining}.
     *
     * <p>This is the building block used by {@link RegexLexer} for token-by-token, mode-aware
     * scanning.  The caller advances its position by {@link MatchResult#length()} after each call.
     *
     * @param remaining      Unscanned portion of the current line (must not be empty).
     * @param lineNum        1-based line number, forwarded to the created {@link Token}.
     * @param lineCharIndex  Absolute character index of the <em>start of the line</em> in the
     *                       source stream (used to build {@link Location}).
     * @param positionInLine Column offset (0-based) of {@code remaining} within the line.
     * @param mode           Current lexer mode name, or {@code null} / {@code "DEFAULT_MODE"}.
     * @return A {@link MatchResult} containing the matched token and its consumed length.
     * @throws LexerException if no terminal matches at the current position.
     */
    public MatchResult matchNext(String remaining, int lineNum, int lineCharIndex,
                                 int positionInLine, String mode) throws LexerException {
        Terminal[] activeTerminals = getActiveTerminalsFor(mode);
        Token bestMatch = null;
        int maxMatchLength = -1;

        for (Terminal terminal : activeTerminals) {
            Matcher matcher = terminal.compiledPattern().matcher(remaining);
            if (matcher.lookingAt()) {
                String name = terminal.tokenName();
                String value = matcher.group(name);
                int matchLength = value.length();
                if (matchLength > maxMatchLength
                        || (matchLength == maxMatchLength && !terminal.isRegex())) {
                    maxMatchLength = matchLength;
                    bestMatch = new Token(terminal, value, new Location(
                            matcher.start(name) + positionInLine,
                            matcher.end(name) + positionInLine,
                            new Line(lineNum, lineCharIndex)));
                }
            }
        }

        if (bestMatch != null) {
            return new MatchResult(bestMatch, maxMatchLength);
        }
        char badChar = remaining.charAt(0);
        String pointer = " ".repeat(positionInLine) + "^";
        throw new LexerException(
                "Unexpected character '" + badChar + "'"
                + " at line " + lineNum + ", column " + positionInLine
                + System.lineSeparator()
                + "  (mode: " + (mode == null ? "DEFAULT_MODE" : mode) + ")"
                + System.lineSeparator()
                + "  " + pointer
        );
    }
}
