package org.twelve.msll.exception;

/**
 * Represents an exception specific to lexing errors in the lexing process.
 * This exception is thrown when the lexer encounters an invalid character or an unexpected token that it cannot recognize or process.
 *
 * @author huizi 2024
 */
public class LexerException extends RuntimeException {
    public LexerException(String message) {
        super(message);
    }
}
