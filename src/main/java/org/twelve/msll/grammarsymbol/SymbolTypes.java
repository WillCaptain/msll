package org.twelve.msll.grammarsymbol;

import java.util.List;

/**
 * Collection interface for symbol types used in grammar representation.
 * It is implemented by two main derived classes: Terminals and NonTerminals.
 * The purpose of this interface is to define operations to manage and access symbol types,
 * including adding new symbols, retrieving symbols by name, and handling symbol collections.
 *
 * @param <T> The type of symbol, which must extend SymbolType.
 */
public interface SymbolTypes<T extends SymbolType> {

    /**
     * Finds and retrieves a symbol by its lexeme name.
     *
     * @param lexeme The name of the symbol to search for.
     * @return The symbol matching the given lexeme name, or null if not found.
     */
    T fromName(String lexeme);

    /**
     * Adds a new symbol type to the collection.
     *
     * @param symbolType The symbol type to add.
     * @return The symbol added, or an existing one if it was already present.
     */
    T addSymbol(T symbolType);

    /**
     * Gets a list of all symbol types in the collection.
     *
     * @return A list of all symbols in the collection.
     */
    List<T> values();

    /**
     * Adds the symbol to the collection if it is not already present.
     *
     * @param symbolType The symbol type to add if absent.
     * @return The existing or newly added symbol.
     */
    T addIfAbsent(T symbolType);
}
