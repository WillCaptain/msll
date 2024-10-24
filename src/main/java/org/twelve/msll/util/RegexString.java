package org.twelve.msll.util;

/**
 * Represents a string that will be used for matching a token via a regular expression.
 * This class is used to encapsulate regex strings, ensuring that they are treated as
 * regex patterns for token matching in a lexer.
 *
 * @author huizi 2024
 */
public class RegexString {

    // Stores the original regex string used for matching tokens.
    private final String origin;

    /**
     * Constructs a {@code RegexString} object with the given regex pattern.
     *
     * @param origin The original regex string, representing the pattern.
     *               The pattern is trimmed to remove any leading or trailing whitespace.
     */
    public  RegexString(String origin){
        this.origin = origin.trim();
    }

    /**
     * Returns the original regex string.
     *
     * @return The regex string used to match tokens.
     */
    @Override
    public String toString() {
        return origin;
    }
}
