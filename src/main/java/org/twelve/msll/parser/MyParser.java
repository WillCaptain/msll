package org.twelve.msll.parser;

import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.lexer.Lexer;
import org.twelve.msll.parsetree.MyParserTree;
import org.twelve.msll.parsetree.NonTerminalNode;
import org.twelve.msll.parsetree.ParseNode;

import java.io.Reader;

import static org.twelve.msll.util.Tool.cast;

/**
 * MyParser is a self designed language parser for handling CFG (Context-Free Grammar) format grammars.
 *
 * The CFG format is generated by converting G4 grammars via `ParserGrammarParser` (for parser grammar)
 * and `LexerGrammarParser` (for lexer grammar) in the MyParser builder. This parser is designed to
 * process grammars for a custom language.
 *
 * @author huizi 2024
 */
public class MyParser extends MsllParser<MyParserTree> {

    /**
     * Constructor for initializing the custom MyParser with grammar rules and a prediction table.
     *
     * @param grammars      The grammar rules in CFG format.
     * @param predictTable  The prediction table used for token matching during parsing.
     * @param nonTerminals  The set of non-terminal symbols in the custom language grammar.
     * @param terminals     The set of terminal symbols (tokens) used in the parsing process.
     * @param reader        The input source to be parsed (usually the source code of the custom language).
     */
    public MyParser(Grammars grammars, PredictTable predictTable, NonTerminals nonTerminals, Terminals terminals, Reader reader) {
        super(grammars, predictTable, nonTerminals, terminals,reader);
    }

    /**
     * Creates the initial parse tree for the self designed language CFG grammar.
     *
     * This method initializes a new MyParserTree, starting with the specified root node. The parse tree represents
     * the hierarchical structure of the custom grammar being parsed, where non-terminal nodes represent grammar rules
     * and terminal nodes represent tokens.
     *
     * @param start The starting node of the parse tree, typically the root non-terminal symbol.
     * @return A MyParserTree representing the parsed CFG grammar.
     */
    @Override
    protected MyParserTree createParseTree(NonTerminalNode start) {
        return new MyParserTree(start);
    }

    /**
     * Finalizes and optimizes the parse tree after parsing is complete.
     *
     * This method calls the parent class's `done` method to obtain the raw parse tree, then applies a series of abstractions
     * to streamline the tree structure. Specifically, it abstracts nodes that have only one child to simplify the overall structure.
     *
     * @return A fully parsed and abstracted MyParserTree.
     */
    @Override
    protected MyParserTree done() {
        MyParserTree tree = super.done();
        this.abstractNodes(tree.start());
        return tree;
    }

    /**
     * Recursively abstracts nodes in the parse tree by removing unnecessary intermediate nodes.
     *
     * This method simplifies the parse tree by abstracting non-terminal nodes that have only one child.
     * It replaces such nodes with their child nodes to flatten the structure. This process is recursive,
     * traversing the entire tree.
     *
     * @param node The current non-terminal node being processed.
     */
    private void abstractNodes(NonTerminalNode node) {
        for (ParseNode n : node.nodes()) {
            if(n instanceof NonTerminalNode){
                abstractNodes(cast(n));
            }
        }
        if(node.nodes().size()==1 && !node.symbol().type().fixed()){
            NonTerminalNode parent = node.parent();
            if(parent!=null) {
                int index = parent.removeNode(node);
                parent.addNode(node.nodes().get(0), index);
            }
        }
    }
}
