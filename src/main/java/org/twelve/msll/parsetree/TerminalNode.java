package org.twelve.msll.parsetree;

import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.lexer.Location;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parser.Symbol;

/**
 * A `TerminalNode` represents a terminal symbol (or token) in the parse tree.
 * It is a specific type of `ParseNode` used to store terminals such as keywords, operators, and identifiers.
 *
 * A `TerminalNode` holds a `Token` object, which contains the actual lexical value and position of the terminal
 * in the source code. This node is created during the parsing process when a terminal is matched by the parser.
 *
 * @author huizi 2024
 */
public class TerminalNode extends ParseNode<Terminal>{
    private Token token;

    /**
     * Constructs a `TerminalNode` for the given terminal symbol.
     * The token is initialized with a default value based on the terminal symbol.
     *
     * @param symbol The terminal symbol this node represents.
     */
    public TerminalNode(Symbol<Terminal> symbol) {
        super(symbol);
        this.token = new Token(this.symbol.type());
    }

    /**
     * Returns the source code location of the token associated with this terminal node.
     * This is useful for error reporting or diagnostics.
     *
     * @return The location of the token in the source code.
     */
    @Override
    public Location location() {
        return this.token.location();
    }

    /**
     * Returns the terminal symbol associated with this node.
     *
     * @return The terminal symbol.
     */
    public Terminal terminal(){
        return this.symbol.type();
    }

    /**
     * Sets the token associated with this terminal node.
     * The token holds the actual lexeme and location of the terminal.
     *
     * @param token The token to set.
     */
    public void setToken(Token token) {
        this.token = token;
    }

    /**
     * Returns the string representation of the token for debugging, typically the lexeme or value.
     *
     * @return The string representation of the token.
     */
    @Override
    public String toString() {
        return this.token.toString();
    }

    /**
     * Returns the lexeme (actual value) of the token.
     * This is the string representation of the terminal symbol in the source code.
     *
     * @return The lexeme of the token.
     */
    @Override
    public String lexeme() {
        return this.token.lexeme();
    }

    /**
     * Creates a clone of this terminal node, copying the terminal symbol and associated token.
     *
     * @return A clone of this terminal node.
     */
    public TerminalNode clone(){
        TerminalNode node = new TerminalNode(this.symbol);
        node.setToken(this.token);
        return node;
    }

    /**
     * Returns the token associated with this terminal node.
     *
     * @return The token object.
     */
    public Token token() {
        return token;
    }
}
