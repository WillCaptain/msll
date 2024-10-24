package org.twelve.msll.lexer;

/**
 * Represents a line in a source file, which is used for indicating the line number and its starting position.
 * The line information is part of the token's position metadata, providing better traceability and
 * error reporting during parsing.
 *
 * Fields:
 * - `beginIndex`: The starting index of the line in the input stream.
 * - `number`: The line number, starting from 1.
 *
 * Methods:
 * - `beginIndex()`: Returns the starting index of the line.
 * - `number()`: Returns the line number.
 *
 * @author huizi 2024
 */
public class Line {
    private final int beginIndex;
    private final int number;

    public Line(int number, int beginIndex) {
        this.beginIndex = beginIndex;
        this.number = number;
    }

    public int beginIndex(){
        return this.beginIndex;
    }

    public int number(){
        return this.number;
    }
}
