package org.twelve.msll.grammarsymbol;

import org.twelve.msll.parser.Symbol;
import org.twelve.msll.parsetree.ParseNode;

/**
 * Represents the type of a symbol, which can either be a terminal or a non-terminal in a grammar.
 *
 * `SymbolType` serves as the base class for defining both terminals (specific tokens from the input)
 * and non-terminals (abstract grammar rules). Each symbol type has a name, and it provides a method
 * to create a parse tree node based on its type.
 *
 * - **Terminal**: Represents concrete tokens such as keywords or punctuation.
 * - **Non-Terminal**: Represents abstract grammar rules that are further broken down into other symbols.
 *
 * This class contains basic functionality such as returning the symbol's name and converting the symbol to a string.
 *
 * @author huizi 2024
 */
public abstract class SymbolType {
    /**
     * The name of the symbol type (e.g., a terminal like "ID" or a non-terminal like "expression").
     */
    protected String name;
    /**
     * Constructs a `SymbolType` with a given name.
     *
     * @param name The name of the symbol type.
     */
    public SymbolType(String name) {
        this.name = name.trim();
    }

    /**
     * Returns the name of the symbol type.
     *
     * @return The name of the symbol type.
     */
    public String name() {
        return this.name;
    }

    /**
     * Creates a parse tree node based on the current symbol type.
     * This method is used to generate the corresponding parse tree node during the parsing process.
     *
     * @param symbol The symbol associated with this type.
     * @return A `ParseNode` that represents this symbol type in the parse tree.
     */
    public abstract ParseNode parse(Symbol symbol);

    @Override
    public String toString() {
        return this.name;
    }
}
