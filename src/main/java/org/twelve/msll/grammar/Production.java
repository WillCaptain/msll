
package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.NonTerminal;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.Symbol;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents a production in a context-free grammar (CFG).
 * A production defines how a non-terminal can be expanded to a sequence of terminals and/or non-terminals.
 * The production is expressed in the form: non-terminal -> production (right-hand side).
 *
 * @author huizi 2024
 */
public class Production implements Iterable<Symbol> {

    /**
     * Stores the sequence of symbols (both terminals and non-terminals) that form the right-hand side of this production.
     */
    private final List<Symbol> symbols = new ArrayList<>();

    /**
     * The associated grammar to which this production belongs.
     */
    private final Grammar grammar;

    /**
     * Specifies the associativity for operators in this production (e.g., LEFT, RIGHT, NONE).
     */
    private final ASSOC assoc;

    /**
     * Display string that represents the production in a readable format.
     */
    private final String display;

    /**
     * Stores the set of terminals in the FIRST set for this production, used in parsing decisions.
     */
    private final Set<Terminal> first = new HashSet<>();

    /**
     * Reference to the collection of non terminals in the grammar.
     */
    private final NonTerminals nonTerminals;

    /**
     * Reference to the collection of terminals in the grammar.
     */
    private final Terminals terminals;

    /**
     * An explanation or comment associated with this production.
     */
    private final String explain;

    /**
     * Indicates whether this production contains left recursion.
     */
    private boolean isLeftRecur = false;

    /**
     * Private constructor to initialize common fields for different ways to create a production.
     *
     * @param grammar    The associated grammar.
     * @param nonTerminals Collection of non-terminals.
     * @param terminals  Collection of terminals.
     * @param display    Display representation of the production.
     * @param explain    Explanation or comment about the production.
     * @param assoc      Associativity for operators.
     */
    private Production(Grammar grammar, NonTerminals nonTerminals, Terminals terminals, String display, String explain, ASSOC assoc) {
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
        this.display = display;
        this.grammar = grammar;
        this.explain = explain;
        this.assoc = assoc;
        grammar.productions().add(this);
    }

    /**
     * Constructs a production from a given grammar and production string.
     *
     * @param grammar    The associated grammar.
     * @param production A string representing the production (sequence of symbols).
     * @param nonTerminals Collection of non-terminals.
     * @param terminals  Collection of terminals.
     */
    public Production(Grammar grammar, String production, NonTerminals nonTerminals, Terminals terminals) {
        this(grammar, nonTerminals, terminals, grammar.name() + "->" + production, "", ASSOC.NONE);

        String[] words = this.formatProductionString(production).trim().split("\\s+");
        for (String word : words) {
            Terminal terminal = terminals.fromName(word);
            if (terminal == null) {
                NonTerminal non = nonTerminals.fromName(word);
                if (non == null) {
                    throw new IllegalArgumentException("symbol: " + word + " is not defined.");
                } else {
                    this.symbols.add(new Symbol<>(non, word));
                }
            } else {
                this.symbols.add(new Symbol<>(terminal, word));
            }
        }
        this.isLeftRecur = this.verifyLeftRecursion();

    }

    /**
     * Constructs a production using a list of symbols.
     *
     * @param grammar    The associated grammar.
     * @param symbols    A list of symbols forming the production.
     * @param nonTerminals Collection of non-terminals.
     * @param terminals  Collection of terminals.
     * @param explain    Explanation or comment about the production.
     * @param assoc      Associativity for operators.
     */
    public Production(Grammar grammar, List<Symbol> symbols, NonTerminals nonTerminals, Terminals terminals, String explain, ASSOC assoc) {
        this(grammar, nonTerminals, terminals, grammar.name() + "->" + StringUtils.parse(symbols), explain, assoc);
        this.symbols.addAll(symbols);
        this.isLeftRecur = this.verifyLeftRecursion();
    }

    /**
     * Constructs a production using a list of symbols.
     * ignore explain and assoc
     *
     * @param grammar    The associated grammar.
     * @param symbols    A list of symbols forming the production.
     * @param nonTerminals Collection of non-terminals.
     * @param terminals  Collection of terminals.
     */
    public Production(Grammar grammar, List<Symbol> symbols, NonTerminals nonTerminals, Terminals terminals) {
        this(grammar, symbols, nonTerminals, terminals, "", ASSOC.NONE);
    }

    /**
     * Verifies if the production contains left recursion.
     * Left recursion occurs when the first symbol on the right-hand side is the same as the left non-terminal.
     *
     * @return True if left recursion is detected, false otherwise.
     */
    private boolean verifyLeftRecursion() {
        Symbol firstSymbol = this.symbols().get(0); // check the first symbol
        return (!firstSymbol.isTerminal() && firstSymbol.name().equals(this.grammar.name()));
    }

    /**
     * Verifies if the production contains left recursion.
     * Left recursion occurs when the first symbol on the right-hand side is the same as the left non-terminal.
     *
     * @return True if left recursion is detected, false otherwise.
     */
    private String formatProductionString(String productionStr) {
        String handledProductionStr = productionStr.replace(";", " " + Constants.SEMICOLON_STR + " ");
        handledProductionStr = handledProductionStr.replace("<", " " + Constants.LESS_STR + " ");
        handledProductionStr = handledProductionStr.replace(">", " " + Constants.GREATER_STR + " ");
        return handledProductionStr;
    }

    /**
     * Returns the associated grammar for this production.
     *
     * @return The grammar to which this production belongs.
     */
    public Grammar grammar() {
        return this.grammar;
    }

    /**
     * Returns the sequence of symbols (terminals and non-terminals) for this production.
     *
     * @return List of symbols in the production.
     */
    public List<Symbol> symbols() {
        return new ArrayList<>(this.symbols);
    }

    /**
     * Checks if the production is an empty production.
     *
     * @return True if the production is an empty production (produces ε), false otherwise.
     */
    public boolean isEmpty() {
        return this.symbols.size() == 1 && this.symbols.get(0).type() == terminals.EPSILON;
    }

    /**
     * Calculates the FIRST set for this production.
     *
     * @return Set of terminals in the FIRST set.
     */
    public Set<Terminal> first() {
        return this.first;
    }

    @Override
    public Iterator<Symbol> iterator() {
        return this.symbols.iterator();
    }

    @Override
    public void forEach(Consumer<? super Symbol> action) {
        for (Symbol symbol : this.symbols) {
            action.accept(symbol);
        }
    }

    @Override
    public String toString() {
        return this.display;
    }

    /**
     * Returns an explanation or comment about the production.
     *
     * @return The explanation for the production.
     */
    public String explain() {
        return this.explain;
    }

    /**
     * Checks if this production contains left recursion.
     *
     * @return True if the production is left-recursive, false otherwise.
     */
    public boolean isLeftRecur() {
        return this.isLeftRecur;
    }

    /**
     * Returns the associativity of the production.
     *
     * @return The associativity type (e.g., LEFT, RIGHT, NONE).
     */
    public ASSOC assoc() {
        return this.assoc;
    }
}
