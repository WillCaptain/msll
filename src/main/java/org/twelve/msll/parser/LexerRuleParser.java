package org.twelve.msll.parser;

import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammarsymbol.NonTerminal;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parsetree.*;
import org.twelve.msll.util.Constants;

import java.io.Reader;

import static org.twelve.msll.util.Tool.cast;

/**
 * LexerGrammarParser is a specialized parser for processing G4 lexer grammar files.
 *
 * This class extends G4Parser to handle the specific structure and rules of a G4 lexer grammar. It constructs a
 * parse tree based on terminal (token) definitions and applies optimizations to simplify the resulting structure.
 *
 * @author huizi 2024
 */
public class LexerRuleParser extends G4Parser<LexerRuleTree> {

    /**
     * LexerGrammarParser is a specialized parser for processing G4 lexer grammar files.
     *
     * This class extends G4Parser to handle the specific structure and rules of a G4 lexer grammar. It constructs a
     * parse tree based on terminal (token) definitions and applies optimizations to simplify the resulting structure.
     */
    public LexerRuleParser(Grammars grammars, PredictTable predictTable, NonTerminals nonTerminals, Terminals terminals, Reader reader) {
        super(grammars, predictTable, nonTerminals, terminals, reader);
    }

    /**
     * Finalizes and optimizes the parse tree after parsing is complete.
     *
     * This method invokes the parent class's `done` method to construct the raw parse tree,
     * then applies optimizations specific to the lexer grammar, such as removing redundant nodes
     * or simplifying recursive structures. The final tree is returned for further processing or analysis.
     *
     * @return A fully parsed and optimized LexerGrammarTree.
     */
    @Override
    protected LexerRuleTree done() {
        LexerRuleTree tree = super.done();
        this.optimizeNodes(tree.start(), cast(tree.grammarRoot()));
        return tree;
    }

    /**
     * Returns the head of the grammar, which is used as the primary non-terminal symbol for the lexer grammar.
     *
     * In this implementation, the head corresponds to the terminal symbol definition (e.g., TERMINAL), which is
     * used to recognize and start processing token rules in the lexer grammar.
     *
     * @return The non-terminal symbol representing the head of the lexer grammar.
     */
    @Override
    protected NonTerminal getHead() {
        return this.nonTerminals.fromName(Constants.TERMINAL);
    }

    /**
     * Creates the initial parse tree for the G4 lexer grammar.
     *
     * This method initializes a new LexerGrammarTree, starting with the specified root node. The parse tree represents
     * the hierarchical structure of the G4 lexer grammar, where each terminal symbol (token) is processed as part of
     * the lexer rules.
     *
     * @param start The starting node of the parse tree, typically the root non-terminal symbol (TERMINAL).
     * @return A LexerGrammarTree representing the parsed lexer grammar.
     */
    @Override
    protected LexerRuleTree createParseTree(NonTerminalNode start) {
        return new LexerRuleTree(start);
    }

}
