package org.twelve.msll.lexer;

import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public class TokenBuffer {
    private final List<Token> tokens = new ArrayList<>();
    private final CodeCache codeCache;
    private int currentIndex = 0;

    /**
     * Set when tokenization aborts on malformed input (e.g. a stray character
     * with no matching terminal). Carries the positioned {@code LexerException}
     * message so the parser can raise a clean {@code GrammarSyntaxException}
     * instead of dereferencing the {@code null} sentinel token that follows it.
     */
    private volatile String lexerError = null;

    public TokenBuffer(CodeCache codeCache) {
        this.codeCache = codeCache;
    }

    /** Records why tokenization aborted; see {@link #lexerError}. */
    public synchronized void setLexerError(String message) {
        this.lexerError = message;
    }

    /** @return the lexer failure message, or {@code null} if tokenization succeeded. */
    public synchronized String lexerError() {
        return this.lexerError;
    }

    // Method to add tokens to the buffer
    public synchronized void addToken(Token token) {
        tokens.add(token);
        notifyAll(); // Notify waiting threads that a new token is available
    }

    public synchronized Token nextToken() throws InterruptedException {
        while (currentIndex >= tokens.size()) {
            wait(); // Wait for tokens to be added
        }
        return tokens.get(currentIndex++);
    }

    public synchronized Token lookahead(int k) {
        int lookaheadIndex = currentIndex + k - 1;
        if (lookaheadIndex < tokens.size()) {
            return tokens.get(lookaheadIndex);
        } else {
            return null; // Or handle EOF
        }
    }

    @SneakyThrows
    public synchronized Token get(Integer index){
        while (index >= tokens.size()) {
            wait();
        }
        return tokens.get(index);
    }

    public int size(){
        return this.tokens.size();
    }

    public String getLine(int number) {
        return this.codeCache.getLine(number);
    }
}
