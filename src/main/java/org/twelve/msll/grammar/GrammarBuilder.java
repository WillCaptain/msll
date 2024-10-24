package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.*;
import org.twelve.msll.parser.Symbol;
import org.twelve.msll.util.Tool;

import java.util.*;

import static org.twelve.msll.util.Tool.cast;

/**
 * Abstract GrammarBuilder class for constructing a set of grammars for a customized language.
 *
 * This class provides methods for creating grammars, managing non-terminals and terminals,
 * and calculating FIRST and FOLLOW sets. It also handles left recursion elimination and grammar verification.
 *
 * huizi 2024
 * @since 1.0
 */
public abstract class GrammarBuilder {
    /**
     * A map to store the grammars by their names
     */
    protected final Map<String, Grammar> grammars = new HashMap<>();
    /**
     * Non-terminal symbols used in the grammar
     */
    protected final NonTerminals nonTerminals;

    /**
     * Terminal symbols used in the grammar
     */
    protected final Terminals terminals;

    /**
     * Constructor to initialize the GrammarBuilder with non-terminal and terminal symbols.
     *
     * @param nonTerminals The set of non-terminal symbols for the grammar.
     * @param terminals    The set of terminal symbols for the grammar.
     */
    protected GrammarBuilder(NonTerminals nonTerminals, Terminals terminals) {
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
    }

    /**
     * Creates or retrieves an existing grammar based on the grammar name.
     *
     * This method checks if the grammar already exists in the map. If it doesn't, it creates a new Grammar
     * object and adds it to the grammars map. It also ensures that the non-terminal associated with the grammar
     * is added to the non-terminal set.
     *
     * @param grammarName The name of the grammar to be created.
     * @return The newly created or existing Grammar object.
     */
    protected Grammar createGrammar(String grammarName) {
        NonTerminal nonTerminal = this.nonTerminals.fromName(grammarName);
        if(nonTerminal==null){
            nonTerminal = this.nonTerminals.addNonTerminal(grammarName);
        }
        Grammar grammar = grammars.get(grammarName);
        if (grammar == null) {
            grammar = new Grammar(nonTerminal, nonTerminals, terminals, grammarName);
            grammars.put(grammarName, grammar);
        }
        return grammar;
    }

    /**
     * Determines whether the FIRST set of a production has changed.
     *
     * This method compares the current size of the production's FIRST set with its previous size
     * after updating it. If the size increases, it indicates that new terminals have been added to
     * the FIRST set, and the method returns true. and the first computation will continue.
     *
     * @param production The production whose FIRST set is being checked.
     * @param changed    The current state of change for FIRST sets.
     * @return True if the FIRST set has changed; otherwise false.
     */
    private boolean isFirstChanged(Production production, boolean changed) {
        boolean isChanged = changed;
        boolean broken = false;
        int oldSize = production.first().size();
        for (Symbol symbol : production.symbols()) {
            broken = checkBroken(production, symbol, broken);
        }
        // if all symbol in production can produce epsilon, then add epsilon to grammar.first
        if (!broken) {
            production.first().add(terminals.EPSILON);
        }
        if (production.first().size() > oldSize) {
            isChanged = true;
        }
        return isChanged;
    }

    /**
     * Checks whether a production can continue to add symbols to the FIRST set.
     *
     * This method processes each symbol in the production, adding its FIRST set to the production's
     * FIRST set. If the symbol is a terminal, it is added directly, and the loop breaks. If the symbol is
     * a non-terminal and contains epsilon, the loop continues to the next symbol.
     *
     * @param production The production being processed.
     * @param symbol     The symbol from the production being examined.
     * @param broken     Whether the production has "broken" (i.e., can no longer add symbols to FIRST).
     * @return True if the production has been "broken" (i.e., can't continue to add symbols to FIRST); otherwise false.
     */
    private boolean checkBroken(Production production, Symbol symbol, boolean broken) {
        boolean isBroken = broken;
        // symbol is a terminal symbol, then FIRST(this) is {symbol}.
        if (symbol.isTerminal()) {
            if (!isBroken) {
                production.first().add(cast(symbol.type()));
                isBroken = true;
            }
        } else {
            // if the first symbol is non-terminal, then this.first == the non-terminal.first
            Grammar grammar = production.grammar().grammars().get(symbol.name());
            if (grammar == null) {
                Tool.grammarError("symbol: " + symbol.name() + " is not found in all possible grammars");
            }
            grammar.trace();
            // if there is epsilon in this symbol FIRST, continue to compute next symbol, the next symbol
            // FIRST will be added to the production as well
            List<Terminal> grammarFirst = grammar.first();
            boolean hasEpsilon = grammarFirst.remove(terminals.EPSILON);
            if (!isBroken) {
                production.first().addAll(grammarFirst);
            }
            // if this is epsilon, go on add the first to production;
            if (!isBroken && !hasEpsilon) {
                isBroken = true; // there is no epsilon in this symbol, do not need to traverse next symbol
            }
        }
        return isBroken;
    }

    /**
     * Abstract method for initializing grammars.
     *
     * This method should be implemented by subclasses to define how the grammars are initialized.
     */
    public abstract void initialize();

    /**
     * Builds the set of grammars after eliminating left recursion, computing FIRST and FOLLOW sets,
     * and verifying the start grammar.
     *
     * @return The constructed Grammars object, ready for parsing.
     */
    public Grammars build() {
        this.eliminateLeftRecur(grammars);
        Grammars gs = new Grammars(grammars);
        verifyStart(gs);
        this.computeFirsts();
        this.computeFollows();
        // print not traced grammars
        printGrammarWarning(gs);
        return gs;
    }

    /**
     * Eliminates left recursion in the grammars.
     *
     * This method processes all grammars and calls their individual methods for eliminating left recursion.
     * check Grammar.eliminateLeftRecur logic to implement left recursion to non left recursion
     *
     * @param grammars The set of grammars to process.
     */
    public void eliminateLeftRecur(Map<String, Grammar> grammars) {
        List<Grammar> all = new ArrayList<>();
        all.addAll(grammars.values());
        all.forEach(g -> g.eliminateLeftRecur(grammars));
    }

    /**
     * 去掉头部所有的terminal，留下尾部第一个terminal
     * trim(abCdef) = Cd;
     *
     * @param origin input symbol list
     * @return trimmed symbol list
     */
    private List<Symbol> trimSProductionSymbols(List<Symbol> origin) {
        List<Symbol> symbols = new ArrayList<>();
        boolean onHead = false;
        List<Symbol> buffer = new ArrayList<>();
        for (Symbol symbol : origin) {
            if (symbol.isTerminal()) {
                if (onHead) {
                    buffer.add(symbol);
                }
            } else {
                if (!onHead) {
                    onHead = true;
                } else {
                    symbols.addAll(buffer);
                    buffer.clear();
                }
                symbols.add(symbol);
            }
        }
        if (buffer.size() > 0) {
            symbols.add(buffer.get(0));
        }
        return symbols;
    }

    /**
     * Computes the FIRST set for all productions in the grammar.
     *
     * This method iterates through all productions and calculates their FIRST sets.
     * It repeats this process until no changes are detected, ensuring that all FIRST sets are correctly computed.
     */
    private void computeFirsts() {
        // get all productions
        List<Production> productions = new ArrayList<>();
        grammars.values().forEach(g -> productions.addAll(g.productions()));
        // traverse productions multi times until there is no change for all grammar.follow
        boolean changed;
        do {
            changed = false;
            for (Production production : productions) {
                changed = isFirstChanged(production, changed);
            }
        } while (changed);
    }

    /**
     * Computes the FOLLOW set for all productions in the grammar.
     *
     * This method iterates through all productions and calculates their FOLLOW sets,
     * repeating the process until no changes are detected.
     */
    private void computeFollows() {
        // get all productions
        List<Production> productions = new ArrayList<>();
        grammars.values().forEach(g -> productions.addAll(g.productions()));
        // traverse productions multi times until there is no change for all grammar.follow
        boolean changed;
        do {
            changed = false;
            for (Production production : productions) {
                changed = isFollowChanged(production, changed);
            }
        } while (changed);
    }

    /**
     * Checks whether the FOLLOW set of a production has changed.
     *
     * This method processes a production's symbols from right to left, updating the FOLLOW set of each non-terminal
     * symbol based on the symbols that follow it. It trims terminal symbols from the head of the production.
     *
     * @param production The production being processed.
     * @param changed    The current state of change for FOLLOW sets.
     * @return True if the FOLLOW set has changed; otherwise false.
     */
    private boolean isFollowChanged(Production production, boolean changed) {
        boolean isChanged = changed;
        Grammar grammar = production.grammar();
        // trim production symbols
        List<Symbol> symbols = this.trimSProductionSymbols(production.symbols());
        Set<Terminal> first = new HashSet<>(); // next first is my follow
        boolean containsEmpty = true;
        for (int i = symbols.size() - 1; i >= 0; i--) {
            Symbol symbol = symbols.get(i);
            // from right to left, if there are terminal from right
            if (symbol.isTerminal()) {
                first.clear();
                first.add(cast(symbol.type()));
                containsEmpty = false;
                continue;
            }
            Grammar g = grammars.get(symbol.name()); // a non-terminal grammar in this production
            int oldSize = g.follow().size();
            // if the rest produce empty, add parent follow to my follow
            if (containsEmpty) {
                g.follow().addAll(grammar.follow());
            }
            // next first add to my follow
            g.follow().addAll(first);
            if (!g.first().contains(terminals.EPSILON)) { // if I don't have epsilon, the previous grammar will only
                // have my first, or it will have my follow
                first.clear();
                containsEmpty = false;
            }
            first.addAll(g.first());
            if (g.follow().size() > oldSize) {
                isChanged = true;
            }
        }
        return isChanged;
    }

    private void printGrammarWarning(Grammars grammars) {
        grammars.grammars()
                .stream()
                .filter(grammar -> !grammar.traced())
                .forEach(g -> Tool.warn("Warning: grammar " + g.name() + " is not traced"));
    }

    /**
     * Verifies that the start grammar is correctly defined and traced.
     *
     * This method ensures that:
     * - There is exactly one start rule defined with the correct non-terminal symbol.
     * - The start rule is properly traced and registered in the grammar.
     * If these conditions are not met, an error is thrown. This step is critical to
     * establishing the entry point for the grammar and ensuring it is well-formed.
     *
     * @param grammars The set of grammars being verified.
     */
    private void verifyStart(Grammars grammars) {
        Grammar start = grammars.getStart();
        if (start == null) {
            Tool.grammarError("there must be one start grammar with name: SCRIPT");
        }
        if (!start.type().isStart()) {
            Tool.grammarError("script grammar must be NonTerminal.SCRIPT");
        }
        if (grammars.grammars().stream().filter(g -> g.type().isStart()).count() > 1) {
            Tool.grammarError("there must be only one start grammar with name: SCRIPT");
        }
        start.trace();
    }

    public NonTerminals nonTerminals() {
        return this.nonTerminals;
    }

    public Terminals terminals() {
        return this.terminals;
    }
}
