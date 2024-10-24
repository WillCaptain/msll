package org.twelve.msll.grammarsymbol;

import org.twelve.msll.exception.LexerException;
import org.twelve.msll.lexer.Line;
import org.twelve.msll.lexer.Location;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.RegexString;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores all terminal symbols for a specific parser.
 * Each terminal includes metadata for token matching, which can either be a full match string or a regex pattern.
 *
 * When matching a token, three principles apply in case of multiple matches:
 * 1. **Maximal Munch**: The longest matching token is selected.
 * 2. **Keyword Priority**: If multiple matches exist, string definitions (keywords) have a higher priority than regex matches.
 * 3. **Order of Definition**: If matches are equivalent by length and priority, the last defined match is selected.
 *
 * @author huizi 2024
 */
public class Terminals implements SymbolTypes<Terminal> {
    protected final List<Terminal> terminals = new ArrayList<>();
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
        if(myTerminals==null){
            myTerminals = new Terminals();
            myTerminals.addTerminal(Constants.STRING, new RegexString("(\\\"[^\"]*\\\")"));
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
            myTerminals.addTerminal(Constants.LESS_STR, Constants.LESS);
            myTerminals.addTerminal(Constants.SLASH_STR, Constants.SLASH);
            myTerminals.addTerminal(Constants.AND_STR, Constants.AND);
            myTerminals.addTerminal(Constants.MOD_STR, Constants.MOD);
            myTerminals.addTerminal(Constants.POWER_STR, Constants.POWER);

            myTerminals.addTerminal(Constants.ENTER, new RegexString("\\S* \\n"));
            myTerminals.addTerminal(Constants.NUMBER, new RegexString("(?:|(?<=[\\=\\<\\>])\\s*-\\s*)\\d+(?:\\.\\d+)?(?![\\.])"));

        }
        return myTerminals;
    }

    /**
     * Method to get terminals for parser grammar parsing
     */
    public synchronized static Terminals parser() {
        if(parserTerminals ==null) {
            parserTerminals = new Terminals();
            parserTerminals.addTerminal(Constants.PARSER_GRAMMAR, new RegexString("^parser\\s+grammar\\b"));
            parserTerminals.addTerminal(Constants.STRING, new RegexString("(?<quote>['\"])[^\"']*\\k<quote>"));

            fillTerminals(parserTerminals);
            parserTerminals.addTerminal(Constants.ID, new RegexString("([a-z_][\\w,']*)"));
            parserTerminals.addTerminal(Constants.UPPER_ID, new RegexString("(\\b[A-Z]\\w*)"));
        }
        return parserTerminals;
    }

    /**
     * Method to get terminals for lexer grammar parsing
     */
    public synchronized static Terminals lexer(){
        if(lexerTerminals ==null){
            lexerTerminals = new Terminals();
            lexerTerminals.addTerminal(Constants.STRING, new RegexString("(?<quote>['\"])[^\"']*\\k<quote>"));
            lexerTerminals.addTerminal(Constants.LEXER_GRAMMAR, new RegexString("^lexer\\s+grammar\\b"));
            lexerTerminals.addTerminal(Constants.ANY, Constants.DOT);
            lexerTerminals.addTerminal(Constants.SPECIAL, new RegexString("\\\\(d|D|b|B|S|s|W|w|\\*|\\?|\\+|\\||\\\\)"));
            lexerTerminals.addTerminal(Constants.LEFT_BRACKET_STR, Constants.LEFT_BRACKET);
            lexerTerminals.addTerminal(Constants.RIGHT_BRACKET_STR, Constants.RIGHT_BRACKET);
            lexerTerminals.addTerminal(Constants.SINGLE_CHARACTER, new RegexString("\\[\\^?(?:\\\\.|[^\\]])+\\]"));

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
        terminals.addTerminal(Constants.PREDICATE, new RegexString("\\{[a-zA-Z0-9_.()\"]*\\}"));
        terminals.addTerminal(Constants.EXPLAIN, new RegexString("#\\s*\\w*"));
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
    }

    /**
     * Matches the given input string to the terminal rules and returns the list of matched tokens.
     *
     * @param line        The input line to be tokenized.
     * @param lineNum     The current line number.
     * @param charIndex   The current character index.
     * @return A list of matched tokens.
     * @throws LexerException if an unexpected character is found.
     */
    public List<Token> match(String line, int lineNum, int charIndex) throws LexerException {
        List<Terminal> terminals = new ArrayList<>();
        terminals.add(Terminal.WHITESPACE);
        terminals.addAll(this.terminals);
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        int length = line.length();

        while (true) {
            String remainingInput = line.substring(position);
            Token bestMatch = null;
            int maxMatchLength = -1;

            // Iterate through all terminals to find the best match
            for (Terminal terminal : terminals) {
                Matcher matcher = Pattern.compile(terminal.regex()).matcher(remainingInput);
                if (matcher.lookingAt()) {
                    String name = terminal.tokenName();
                    String value = matcher.group(name);
                    int matchLength = value.length();
                    if (matchLength > maxMatchLength||(matchLength == maxMatchLength && !terminal.isRegex())) {
                        maxMatchLength = matchLength;
                        bestMatch = new Token(terminal, value,new Location(matcher.start(name)+position, matcher.end(name)+position, new Line(lineNum, charIndex)));
                    }
                    // If same length, prioritize the first defined terminal
                    else if (matchLength == maxMatchLength && bestMatch != null) {
                        // Assuming terminals are ordered by priority
                        // Do nothing, as bestMatch is already the first terminal with this length
                    }
                }
            }

            if (bestMatch != null) {
//                if (!bestMatch.terminal().name().equals(Constants.WHITESPACE_STR)||this.keepWhiteSpace) {
                if (!bestMatch.terminal().name().equals(Constants.WHITESPACE_STR)) {
                    tokens.add(bestMatch);
                }
                position = bestMatch.location().lineEnd();
                if(bestMatch.terminal().name.equals(Constants.EOL_STR) && position == length) break;
            } else {
                throw new LexerException("Unexpected character at position " + position + ": '" + line.charAt(position) + "'");
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
            old = this.fromPattern(symbolType.pattern());
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
        return old;
    }

    public Terminal addTerminal(String name, String pattern) {
        Terminal terminal = new Terminal(name, pattern);
        this.terminals.add(terminal);
        return terminal;
    }

    public Terminal addTerminal(String name, RegexString rStr) {
        Terminal terminal = new Terminal(name, rStr);
        this.terminals.add(terminal);
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
        }
        return old;
    }
}
