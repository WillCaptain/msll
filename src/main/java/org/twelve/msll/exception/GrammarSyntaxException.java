package org.twelve.msll.exception;

import org.twelve.msll.parser.MsllStack;

/**
 * This class is used to throw an exception when a syntax error occurs during parsing.
 * It extends {@link RuntimeException} to provide additional information
 * about the error source, specifically in the context of MSLL parsing.
 *
 * @author huizi 2024
 */
public class GrammarSyntaxException extends RuntimeException {

    /**
     * The stack associated with the grammar error. This helps identify the specific
     * context or parsing branch where the error occurred.
     */
    private final MsllStack source;

    /**
     * Constructs a new {@code GrammarSyntaxException} with the specified detail message.
     *
     * @param info the detail message explaining the syntax error.
     */
    public GrammarSyntaxException(String info) {
        this(null,info);
    }

    /**
     * Constructs a new {@code GrammarSyntaxException} with the specified source stack
     * and detail message.
     *
     * @param source the {@link MsllStack} that caused the error, providing context about
     *               the parsing failure.
     * @param info   the detail message explaining the syntax error.
     */
    public GrammarSyntaxException(MsllStack source, String info) {
        super(info);
        this.source = source;
    }

    /**
     * Gets the source stack associated with the syntax error.
     *
     * @return the {@link MsllStack} providing context about the error, or {@code null}
     *         if no specific source stack was provided.
     */
    public MsllStack source() {
        return source;
    }
}
