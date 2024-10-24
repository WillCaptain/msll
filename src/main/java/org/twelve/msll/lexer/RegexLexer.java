package org.twelve.msll.lexer;

import lombok.SneakyThrows;
import org.twelve.msll.exception.LexerException;
import org.twelve.msll.grammarsymbol.Terminals;

import java.io.Reader;
import java.util.ArrayList;
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
        super(reader,terminals);
    }

    private int preCharIndex = 0;
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
     * Uses terminals to match and generate tokens for a given line of input.
     *
     * @param line        The input line to be tokenized.
     * @param currentLine The current line number in the input.
     * @param currentIndex The current character index in the input.
     * @return A list of tokens generated from the input line.
     */
    private List<Token> dealWithTokens(String line, int currentLine, int currentIndex) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        for (Token token : this.terminals.match(line, currentLine, currentIndex)) {
            tokens.add(token);
        }

        return tokens;
    }
}
