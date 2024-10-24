package org.twelve.msll.lexer;

import java.io.Serializable;

/**
 * Represents the position of a node in the source code.
 * It stores the starting and ending positions of a node, along with the associated line information.
 * This information helps in pinpointing the exact location of tokens and nodes during parsing,
 * which is particularly useful for error reporting and debugging.
 *
 * Fields:
 * - `start`: The starting position of the node, adjusted with respect to the line's starting index.
 * - `end`: The ending position of the node, also adjusted with respect to the line's starting index.
 * - `line`: The `Line` object representing the line of code in which the node exists.
 *
 * Methods:
 * - `start()`: Returns the adjusted starting position of the node in the source.
 * - `lineStart()`: Returns the starting position of the node relative to the beginning of its line.
 * - `end()`: Returns the adjusted ending position of the node in the source.
 * - `lineEnd()`: Returns the ending position of the node relative to the beginning of its line.
 * - `line()`: Returns the `Line` object, providing information about the line number and start index.
 *
 * @author huizi 2024
 */
public class Location implements Serializable {
    private static final long serialVersionUID = -8804467929479718489L;
    private final int start;
    private final int end;
    private final Line line;

    /**
     * Constructor for initializing the Location of a node in the source code.
     *
     * @param start Starting position relative to the line's beginning.
     * @param end   Ending position relative to the line's beginning.
     * @param line  Line object representing the line number and starting index in the source.
     */
    public Location(int start, int end, Line line) {
        this.start = start + line.beginIndex();
        this.end = end + line.beginIndex();
        this.line = line;
    }

    /**
     * Gets the absolute starting position of the node in the source.
     *
     * @return Absolute start position.
     */
    public int start() {
        return this.start;
    }

    /**
     * Gets the starting position of the node relative to the beginning of the line.
     *
     * @return Start position relative to line's start.
     */
    public int lineStart(){
        return  this.start()-this.line().beginIndex();
    }

    /**
     * Gets the absolute ending position of the node in the source.
     *
     * @return Absolute end position.
     */
    public int end() {
        return this.end;
    }

    /**
     * Gets the ending position of the node relative to the beginning of the line.
     *
     * @return End position relative to line's start.
     */
    public int lineEnd(){
        return this.end()-this.line().beginIndex();
    }

    /**
     * Gets the Line object, providing information about the line of the source code.
     *
     * @return Line object.
     */
    public Line line() {
        return this.line;
    }
}
