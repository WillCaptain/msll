package org.twelve.msll.parser;

import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.SymbolType;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.util.Constants;

/**
 * Represents a symbol in the grammar, which can either be a terminal or a non-terminal.
 *
 * The `Symbol` class is used as the base type for elements in a production rule. Each symbol has a name and a type,
 * where the type can be either a terminal or a non-terminal.
 *
 * - **Terminals**: These represent actual tokens from the input (such as keywords or operators).
 * - **Non-Terminals**: These represent grammar rules and abstract structures (such as expressions or statements).
 *
 * The `IGNORE` symbol can have a special name format using '***', indicating it is ignored in the parsing process.
 *
 * @param <T> The type of the symbol (can be a terminal or non-terminal).
 *
 * @author huizi 2024
 */
public class Symbol<T extends SymbolType> {
    /**
     * The type of the symbol, either terminal or non-terminal.
     */
    protected final T type;

    /**
     * The name of the symbol, corresponding to its identifier in the grammar.
     */
    private final String name;

    /**
     * Constructs a symbol with a given type and name.
     *
     * @param type The type of the symbol (terminal or non-terminal).
     * @param name The name of the symbol.
     */
    public Symbol(T type, String name) {
        this.type = type;
        this.name = name.trim();
    }

    /**
     * Constructs a symbol using its type name.
     *
     * @param type The type of the symbol.
     */
    public Symbol(T type) {
        this(type,type.name());
    }

    /**
     * Determines if the symbol is a terminal.
     *
     * @return true if the symbol is a terminal, false if it is a non-terminal.
     */
    public boolean isTerminal() {
        return type() instanceof Terminal;
    }

    /**
     * Returns the type of the symbol.
     *
     * @return The symbol's type (terminal or non-terminal).
     */
    public T type() {
        return type;
    }

    /**
     * Returns the name of the symbol.
     *
     * @return The name of the symbol.
     */
    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name() + (this.type == NonTerminals.IGNORED ? "("+ Constants.IGNORED +")" : "");
    }
}
