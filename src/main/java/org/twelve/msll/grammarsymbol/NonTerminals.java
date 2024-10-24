package org.twelve.msll.grammarsymbol;

import org.twelve.msll.util.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages all non-terminals for a given parser.
 * Each non-terminal is represented with metadata required for grammar parsing.
 * Similar to the `Terminals` class, `NonTerminals` stores and provides access to non-terminal symbols.
 * This class supports three contexts: parser non-terminals, lexer non-terminals, and custom non-terminals.
 * It maintains a special 'IGNORED' non-terminal for nodes that should not appear in the final parse tree.
 *
 * @author huizi 2024
 */
public class NonTerminals implements SymbolTypes<NonTerminal> {
    private static NonTerminals parser = null;

    /**
     * Provides singleton instance for ParserGrammarParser non-terminals.
     *
     * @return A singleton instance of `NonTerminals` for parsing g4 parser grammar.
     */
    public synchronized static NonTerminals parser() {
        if(parser ==null) {
            parser = new NonTerminals();
        }
        return parser;
    }
    private static NonTerminals my = null;
    /**
     * Provides singleton instance for self defined language parser non-terminals.
     *
     * @return A singleton instance of `NonTerminals` for customized language.
     */
    public synchronized static NonTerminals my() {
        if(my==null){
            my = new NonTerminals();
        }
        return my;
    }
    private static NonTerminals lexer = null;

    /**
     * Provides singleton instance for LexerRuleParser non-terminals.
     *
     * @return A singleton instance of `NonTerminals` for lexer rules.
     */
    public synchronized static NonTerminals lexer() {
        if(lexer ==null){
            lexer = new NonTerminals();
        }
        return lexer;
    }
    public static final NonTerminal IGNORED = new NonTerminal(Constants.IGNORED);

    protected Map<String, NonTerminal> nonTerminals = new HashMap<>();
    private NonTerminal start = null;

    /**
     * Finds and retrieves a non-terminal by its name.
     *
     * @param name The name of the non-terminal.
     * @return The corresponding `NonTerminal`, or `IGNORED` if it ends with `'`.
     */
    @Override
    public NonTerminal fromName(String name) {
        if (name.endsWith("'")) {
            return IGNORED;
        }
        return nonTerminals.get(name);
    }

    /**
     * Adds a new non-terminal symbol to the collection.
     *
     * @param symbolType The non-terminal to be added.
     * @return The non-terminal that has been added.
     */
    @Override
    public NonTerminal addSymbol(NonTerminal symbolType) {
        if (this.start == null && symbolType.isStart()) {
            this.start = symbolType;
        }
        this.nonTerminals.put(symbolType.name(), symbolType);
        return symbolType;
    }

    /**
     * Gets a list of all non-terminal values in the collection.
     *
     * @return A list of all `NonTerminal` instances.
     */
    @Override
    public List<NonTerminal> values() {
        return this.nonTerminals.values().stream().collect(Collectors.toList());
    }

    /**
     * Adds a non-terminal by name. It may also be marked as the start symbol.
     *
     * @param nonTerminal The name of the non-terminal.
     * @param isStart     Whether it should be the start symbol.
     * @return The `NonTerminal` that was added.
     */
    public NonTerminal addNonTerminal(String nonTerminal, boolean isStart){
        NonTerminal non = new NonTerminal(nonTerminal, isStart);
        return this.addIfAbsent(non);
    }

    /**
     * Adds a non-terminal by name.
     *
     * @param nonTerminal The name of the non-terminal.
     * @return The `NonTerminal` that was added.
     */
    public NonTerminal addNonTerminal(String nonTerminal){
        boolean isStart = this.getStart()==null;
        return this.addNonTerminal(nonTerminal,isStart);
    }

    /**
     * Gets the start non-terminal if defined.
     *
     * @return The start `NonTerminal`.
     */
    public NonTerminal getStart() {
        return this.start;
    }

    /**
     * Adds a non-terminal if it is not already present.
     *
     * @param symbolType The non-terminal to be added if absent.
     * @return The existing or newly added `NonTerminal`.
     */
    @Override
    public NonTerminal addIfAbsent(NonTerminal symbolType) {
        NonTerminal non = this.fromName(symbolType.name());
        if(non==null){
            non = symbolType;
            this.addSymbol(non);
        }
        return non;
    }
}
