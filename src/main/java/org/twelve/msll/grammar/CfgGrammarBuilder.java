package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CfgGrammarBuilder is a specialized class for building CFG (Context-Free Grammar) structures
 * from a given text-based CFG format
 *
 * This class extends GrammarBuilder and processes CFG definitions from an array of strings.
 * Each string represents a grammar rule in the format "non-terminal -> production1 | production2".
 *
 * huizi 2024
 */
public class CfgGrammarBuilder extends GrammarBuilder {
    private final String[] cfgLines;

    /**
     * Constructor for initializing the CfgGrammarBuilder with CFG lines and grammar symbols.
     *
     * This constructor takes an array of CFG rules in text format, along with sets of non-terminals and terminals.
     * It passes the non-terminals and terminals to the parent class (GrammarBuilder) for grammar management.
     *
     * @param cfgLines     The array of CFG rules in textual format.
     * @param nonTerminals The set of non-terminal symbols for the grammar.
     * @param terminals    The set of terminal symbols for the grammar.
     */
    public CfgGrammarBuilder(String[] cfgLines, NonTerminals nonTerminals, Terminals terminals) {
        super(nonTerminals, terminals);
        this.cfgLines = cfgLines;
    }

    /**
     * Creates production rules for a given grammar from a text-based expression.
     *
     * This method splits the right-hand side of a CFG rule (the expression) into individual productions
     * using the "|" delimiter. It then creates a Production object for each production string,
     * associating it with the grammar's non-terminal.
     *
     * "non-terminal -> production1 | production2" to Production(production1) and Production(production2)
     *
     * @param expression The right-hand side of the grammar rule (productions).
     * @param grammar    The Grammar object representing the non-terminal.
     */
    private void createProductions(String expression, Grammar grammar) {
        String[] ps = expression.split("\\s*\\|\\s*");
        for (String p : ps) {
            new Production(grammar, p, this.nonTerminals, this.terminals);
        }
    }

    /**
     * Initializes the CFG grammar by parsing the text-based CFG rules: "non-terminal -> production1 | production2"
     *
     * This method splits each line of the CFG into non-terminal and production components
     * using the "->" delimiter. It first creates the grammars for the non-terminals and then
     * creates the corresponding production rules for each grammar.
     */
    @Override
    public void initialize() {
        // Split each line into non-terminal and production parts
        List<String[]> ps = Arrays.stream(cfgLines)
                .map(l -> l.split("->"))// Split by "->" to separate non-terminal and productions.
                .collect(Collectors.toList());
        // First, create the grammar objects for all non-terminals.
        for (String[] p : ps) {
            createGrammar(p[0].trim());// Create grammar for each non-terminal.
        }
        // Then, create the production rules for each grammar.
        for (String[] p : ps) {
            createProductions(p[1],grammars.get(p[0].trim()));// Create productions for the corresponding non-terminal.
        }
    }
}
