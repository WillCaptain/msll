/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2023-2023. All rights reserved.
 */

package org.twelve.msll.grammar;

import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.NonTerminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.Symbol;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Context-Free Grammar (CFG) for parsing a language.
 * Each Grammar object corresponds to a non-terminal in the language and stores
 * its associated production rules, which define how the non-terminal can be derived.
 *
 * Features include computing FIRST and FOLLOW sets, and eliminating left recursion.
 * This is useful for building parsers, especially LL-based parsers.
 *
 * @author huizi 2024
 */
public class Grammar {
    // Non-terminal associated with this grammar
    private final NonTerminal nonTerminal;

    // Production rules for this non-terminal: non-terminal -> [productions]
    private final List<Production> productions = new ArrayList<>();

    // Collection of all terminal symbols
    private final Terminals terminals;

    // Name of the non-terminal symbol
    private final String name;
    // Collection of all non-terminal symbols
    private final NonTerminals nonTerminals;

    // Collection reference to all defined grammars
    private Grammars grammars;

    // FOLLOW set for this grammar
    private Set<Terminal> follow;

    // Boolean flag indicating if this grammar has been traced (used for debugging)
    private boolean traced = false;

    /**
     * 构造函数
     *
     * @param nonTerminal 非终结符对象
     */
    public Grammar(NonTerminal nonTerminal, NonTerminals nonTerminals, Terminals terminals, String name) {
        this.nonTerminal = nonTerminal;
        this.name = name;
        this.follow = new HashSet<>();
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
        if (nonTerminal.isStart()) {
            this.follow.add(terminals.END);
        }
    }

    /**
     * Gets the non-terminal associated with this grammar
     *
     * @return Non Terminal Symbol
     */
    public NonTerminal type() {
        return this.nonTerminal;
    }

    /**
     * Returns the list of production rules for the grammar
     *
     * @return production list
     */
    public List<Production> productions() {
        return this.productions;
    }

    /**
     * Returns the name of the grammar (non-terminal)
     *
     * @return non terminal name
     */
    public String name() {
        if (this.nonTerminal == NonTerminals.IGNORED) {
            return this.name;
        } else {
            return this.nonTerminal.name();
        }
    }

    /**
     * Boolean flag indicating if this grammar has been traced
     *
     * @return is traced
     */
    public boolean traced() {
        return this.traced;
    }

    /**
     * Computes the FIRST set for the production rules.
     * For each production, calculates its FIRST set and adds it to the non-terminal's FIRST set.
     * If there are overlapping terminal symbols across different productions, an exception is thrown.
     * Note that LL parsers do not support overlapping grammars.
     *
     * @return A list of terminals representing the combined FIRST set of all productions.
     */
    public List<Terminal> first() {
        Set<Terminal> first = new HashSet<>();
        this.productions().forEach(production -> {
            List<Terminal> intersection = new ArrayList<>(first);
            intersection.retainAll(production.first());
            intersection.removeIf(t -> t == terminals.EPSILON);
            if (!intersection.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                intersection.forEach(t -> sb.append(t.name()).append(" "));
//                Tool.grammarError("there is duplicate production first: " + sb + "in grammar: " + this.name()
//                        + ", which will cause FIRST-FIRST conflict in LL(1) parser");
            }
            first.addAll(production.first());
        });
        return new ArrayList<>(first);
    }

    public boolean containsEmptyFirst() {
        return this.first().contains(terminals.EPSILON);
    }

    /**
     * Computes the FOLLOW set for the non-terminal.
     * For each non-terminal, calculates its FOLLOW set and adds it to the non-terminal's FOLLOW set.
     * If the production for a non-terminal can generate an empty string (EPSILON),
     * the FIRST set of the production is added to the FOLLOW set of the non-terminal.
     * If the non-terminal is the start symbol, the terminal END is added to its FOLLOW set.
     *
     * @return A set of terminals representing the FOLLOW set of the non-terminal.
     */
    public Set<Terminal> follow() {
        if (follow == null) {
            follow = new HashSet<>();
        }
        follow.removeIf(t -> t == terminals.EPSILON);
        return this.follow;
    }

    /**
     * Sets the reference to the entire grammar collection.
     *
     * Relationship: The Grammars class is a collection or container that includes multiple Grammar objects,
     * representing the entire grammar. Each Grammar object can reference and relate to other Grammar objects
     * through this grammars reference.
     *
     * @param grammars A reference to the entire grammar collection.
     */
    public void setGrammars(Grammars grammars) {
        this.grammars = grammars;
    }

    /**
     * Retrieves a reference to the entire grammar collection.
     * <p>
     * Relationship: The Grammars class is a collection or container consisting of multiple Grammar objects,
     * representing the entire grammar. Each Grammar object can reference and relate to other Grammar objects
     * through this grammars reference.
     *
     * @return A reference to the entire grammar collection.
     */
    public Grammars grammars() {
        return this.grammars;
    }

    /**
     * Sets the trace status of the non-terminal.
     *
     * Relationship: When debugging or printing information about the non-terminal, its trace status can be set to true.
     */
    public void trace() {
        this.traced = true;
    }

    public NonTerminal nonTerminal() {
        return this.nonTerminal;
    }

    @Override
    public String toString() {
        return this.productions().stream().map(p -> p.toString()).reduce((a, b) -> a + "|" + b).get();
    }

    /**
     * take A → Aα1 |Aα2 |...|Aαn | β1 | β2 |...|βn
     * as Aαs | β
     * β is an abstraction grammar of all non left recursion production: β → β1 | β2 |...|βn
     * Aαs are all left recursion productions, including Aα1,Aα2....Aαn
     * α is all left recursion rest part of the production follows the left recursion non terminal
     * for each Aαi:
     * A  → β Ai'
     * Ai' → αi Ai' | ε
     */
    public void eliminateLeftRecur(Map<String, Grammar> grammars) {
        if (!this.productions.stream().anyMatch(p -> p.isLeftRecur())) return;
        //β → β1 | β2 |...|βn
        Grammar beta = abstractBeta(grammars,this.productions.stream().filter(p -> !p.isLeftRecur()).collect(Collectors.toList()));
        //A → Aα1 |Aα2 |...|Aαn
        List<Production> lefts = this.productions.stream().filter(p -> p.isLeftRecur()).collect(Collectors.toList());
        //A  → β Ai'
        //Ai' → αi Ai' | ε
        for(int i=0; i<lefts.size(); i++) {
            Production left = lefts.get(i);
            //Ai' → αi Ai' | ε
            Grammar ai = abstractAi(grammars,left,i+1);
            //A  → β Ai'
            List<Symbol> bAi = new ArrayList<>();
            bAi.add(new Symbol<>(beta.nonTerminal, beta.name()));
            bAi.add(new Symbol<>(ai.nonTerminal,ai.name()));
            new Production(this, bAi, nonTerminals, terminals);
        }
    }

    /**
     * Ai' → αi Ai' | ε
     */
    private Grammar abstractAi(Map<String, Grammar> grammars, Production left, int idx) {
        String aName = this.name+"_alpha_"+idx+"'";
        Grammar a = this.addGrammar(grammars, aName);
        //αi Ai'
        List<Symbol> symbols = left.symbols();
        symbols.remove(0);
        symbols.add(new Symbol(nonTerminal,aName));
        new Production(a,symbols,nonTerminals,terminals);
        List<Symbol> epsilonSymbols = new ArrayList<>();
        epsilonSymbols.add(new Symbol<>(this.terminals.EPSILON));
        new Production(a,epsilonSymbols,nonTerminals,terminals);
        left.grammar().productions().remove(left);
        return a;
    }

    /**
     * β → β1 | β2 |...|βn
     */
    private Grammar abstractBeta(Map<String, Grammar> grammars, List<Production> productions) {
        String betaName = this.name + "_beta'";
        Grammar beta = this.addGrammar(grammars,betaName);
        for (Production production : productions) {
            new Production(beta,production.symbols(),nonTerminals,terminals);
            this.productions.remove(production);
//            production.changeGrammar(beta);
        }
        return beta;
    }

    private Grammar addGrammar(Map<String, Grammar> grammars, String name){
        NonTerminal nonTerminal = this.nonTerminals.addNonTerminal(name);
        Grammar beta = new Grammar(nonTerminal, this.nonTerminals, this.terminals, name);
        grammars.put(name,beta);
        return beta;
    }
}
