package org.twelve.msll.lexer;

/**
 * Holds the result of a single-token match attempt by {@link org.twelve.msll.grammarsymbol.Terminals#matchNext}.
 *
 * @author huizi 2024
 */
public class MatchResult {
    private final Token token;
    private final int length;

    public MatchResult(Token token, int length) {
        this.token = token;
        this.length = length;
    }

    /** The matched token. */
    public Token token() {
        return token;
    }

    /** The number of characters consumed from the input string. */
    public int length() {
        return length;
    }
}
