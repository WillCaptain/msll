package org.twelve.msll.lexer;

import lombok.SneakyThrows;
import org.twelve.msll.exception.LexerException;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.util.Constants;

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lexer implementation for MSLL.
 * This lexer does not use a DFA for terminal parsing; instead, it uses regular expressions to match terminal symbols.
 * The terminal matching logic is defined in `Terminals.match()`, where regular expressions are used to match the
 * first token of the input string.
 * The `RegexLexer` processes each line of the input by reading characters sequentially and scanning each line to
 * identify and create tokens that match grammar terminals.
 *
 * Characteristics of RegexLexer:
 * - **No DFA**: Unlike traditional lexers, `RegexLexer` doesn't build a deterministic finite automaton (DFA) to recognize terminals.
 *   Instead, it uses regular expressions directly for matching, simplifying the lexer creation but potentially
 *   sacrificing performance.
 * - **Tokenization Flow**: Characters are processed line by line. Once a line is identified, tokens are generated using
 *   regex matching, and then provided to the parser.
 *
 * Methods Overview:
 * - `tokenize()`: Processes each character of the input. Upon reaching a line ending or EOF, it triggers token generation.
 * - `scan()`: Reads the given line and generates tokens using regex matching.
 * - `dealWithTokens()`: Uses the defined terminals to identify and create tokens for the input line.
 *
 * Note: This lexer is not as performant as a DFA-based lexer but offers more flexibility for grammar definitions using regex.
 *
 * @author huizi 2024
 */
public class RegexLexer extends Lexer {
    public RegexLexer(Reader reader, Terminals terminals) {
        super(reader, terminals);
    }

    private int preCharIndex = 0;

    // -----------------------------------------------------------------------
    // Lexer-mode state machine
    // -----------------------------------------------------------------------
    /** Current active lexer mode. Starts in G4's DEFAULT_MODE. */
    private String currentMode = "DEFAULT_MODE";
    /** Mode stack for pushMode / popMode operations. */
    private final Deque<String> modeStack = new ArrayDeque<>();
    @SneakyThrows
    @Override
    protected void tokenize(char ch, int charIndex, int lineIndex, Consumer<Token> consumer){
        char eof = '\uFFFF';
        if (isEnter(ch) || ch == eof) {
            if(codeCache.isMultiLine() && isEnter(ch)) return;
            String line = codeCache.getLine(lineIndex);
            if (!line.isEmpty()) {
                List<Token> tokens = this.scan(line, lineIndex, preCharIndex);
                for (Token token : tokens) {
                    consumer.accept(token);
                }
                preCharIndex = charIndex;
            }
        }
        if (ch == eof) {
            consumer.accept(new Token(this.terminals.END, "\\$", new Location(charIndex, charIndex, new Line(lineIndex, charIndex))));
        }
    }


    /**
     * Reads the given line and generates a list of tokens.
     *
     * @param line        The line to tokenize.
     * @param currentLine The current line number in the input.
     * @param currentIndex The starting index for the line in the input.
     * @return The list of tokens generated from the line.
     */
    private List<Token> scan(String line, int currentLine, int currentIndex) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        tokens.addAll(dealWithTokens(line, currentLine, currentIndex));
        return tokens;
    }

    /**
     * Tokenises one line in a token-by-token loop so that lexer-mode transitions
     * (pushMode / popMode / type) take effect <em>within</em> the same line.
     *
     * <p>For grammars that do not use lexer modes all terminals have
     * {@code mode == null}, which causes {@link Terminals#matchNext} to fall back
     * to the global terminal array — preserving full backward compatibility.
     *
     * @param line         The input line to be tokenised.
     * @param currentLine  Current 1-based line number.
     * @param currentIndex Absolute character index of the start of this line.
     * @return Ordered list of non-whitespace tokens found in the line.
     */
    private List<Token> dealWithTokens(String line, int currentLine, int currentIndex) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (true) {
            // EOL ($) matches the empty string, so we use an unconditioned loop
            // and break only when we see it (mirrors the original match() logic).
            String remaining = line.substring(position);
            MatchResult result = this.terminals.matchNext(remaining, currentLine, currentIndex,
                    position, currentMode);

            Token token = result.token();

            if (!token.terminal().name().equals(Constants.WHITESPACE_STR)) {
                // Execute mode-altering commands inline so the very next matchNext call
                // already uses the updated mode.  Channel / skip commands are still
                // handled by Lexer.handleToken() after the token reaches the buffer.
                String command = token.terminal().getCommand();
                if (command != null) {
                    applyModeCommands(command, token);
                }
                tokens.add(token);
            }

            // EOL ($) is a zero-length sentinel; always exits the loop
            if (token.terminal().name().equals(Constants.EOL_STR)) break;

            position += result.length();
        }
        return tokens;
    }

    /**
     * Parses the lexer-command string (e.g. {@code "-> pushMode(TEMPLATE), channel(HIDDEN)"})
     * and applies only the <em>mode-mutating</em> sub-commands inline.
     *
     * <p>Channel and skip commands are intentionally left for {@link Lexer#handleToken} so
     * that no command is executed twice.
     */
    private void applyModeCommands(String commandStr, Token token) {
        // commandStr looks like "-> pushMode(TEMPLATE)" or "-> type(BackTick), popMode"
        String body = commandStr.startsWith("->") ? commandStr.substring(2) : commandStr;
        for (String part : body.split(",")) {
            String cmd = part.trim();
            if (cmd.startsWith("pushMode")) {
                int s = cmd.indexOf('('), e = cmd.indexOf(')');
                if (s >= 0 && e > s) {
                    modeStack.push(currentMode);
                    currentMode = cmd.substring(s + 1, e).trim();
                }
            } else if (cmd.startsWith("popMode")) {
                if (!modeStack.isEmpty()) currentMode = modeStack.pop();
            } else if (cmd.startsWith("type")) {
                int s = cmd.indexOf('('), e = cmd.indexOf(')');
                if (s >= 0 && e > s) {
                    String typeName = cmd.substring(s + 1, e).trim();
                    Terminal newTerminal = this.terminals.fromName(typeName);
                    if (newTerminal != null) token.relabelAs(newTerminal);
                }
            }
            // channel() and skip are left to Lexer.handleToken()
        }
    }
}
