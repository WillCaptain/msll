package org.twelve.msll.parsetree;


import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.Symbol;

/**
 * Represents the End of File (EOF) node in the parser.
 * This node is a terminal node that signifies the end of the input stream.
 * It is typically added to the stack before parsing begins to ensure the parser knows where
 * the input ends, preventing any parsing beyond the valid input.
 *
 * The `EndNode` is essential for managing termination conditions during the parsing process.
 * It helps detect when the parser has successfully reached the end of the input.
 *
 * @author huizi 2024
 */
public class EndNode extends TerminalNode {
    /**
     * Constructs an `EndNode` object.
     * The `EndNode` represents the end of the input (EOF) terminal symbol.
     *
     * @param terminals The collection of terminal symbols from which the EOF symbol is derived.
     */
    public EndNode(Terminals terminals) {
        super(new Symbol<>(terminals.END));
    }
}
