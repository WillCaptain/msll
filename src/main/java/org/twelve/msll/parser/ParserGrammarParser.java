package org.twelve.msll.parser;

import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammarsymbol.NonTerminal;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.parsetree.ParserGrammarTree;
import org.twelve.msll.parsetree.NonTerminalNode;
import org.twelve.msll.parsetree.TerminalNode;
import org.twelve.msll.util.Constants;

import java.io.Reader;

import static org.twelve.msll.util.Tool.cast;

/**
 * ParserGrammarParser is a specialized parser for processing G4 parser grammar files.
 *
 * It extends the G4Parser class, which translates a grammar in G4 format into a CFG (Context-Free Grammar) format,
 * suitable for parsing. This parser builds a parse tree and applies optimizations specific to G4 parser grammar.
 *
 * @author huizi 2024
 */
public class ParserGrammarParser extends G4Parser<ParserGrammarTree> {
    /**
     * Constructor for initializing the ParserGrammarParser with grammar rules and a prediction table.
     *
     * @param grammars      The grammar rules that define the G4 parser grammar.
     * @param predictTable  The prediction table used for token matching during parsing.
     * @param nonTerminals  The set of non-terminal symbols in the grammar.
     * @param terminals     The set of terminal symbols used in the parsing process.
     * @param reader        The input source to be parsed (usually a G4 parser grammar file).
     */
    public ParserGrammarParser(Grammars grammars, PredictTable predictTable, NonTerminals nonTerminals, Terminals terminals, Reader reader) {
        super(grammars, predictTable, nonTerminals, terminals, reader);
    }

    /**
     * Returns the head of the grammar, which is used as the primary non-terminal symbol for the parser grammar.
     *
     * In this implementation, the head corresponds to the non-terminal symbol for grammar rules (e.g., NON_TERMINAL).
     * This allows the parser to recognize and start processing grammar rules from the root of the G4 parser grammar.
     *
     * @return The non-terminal symbol representing the head of the parser grammar.
     */
    @Override
    protected NonTerminal getHead() {
        return this.nonTerminals.fromName(Constants.NON_TERMINAL);
    }

    /**
     * Creates the initial parse tree for the G4 parser grammar.
     *
     * This method initializes a new ParserGrammarTree, starting with the specified root node. The parse tree represents
     * the hierarchical structure of the G4 grammar being parsed, with non-terminal nodes representing grammar rules and
     * terminal nodes representing tokens.
     *
     * @param start The starting node of the parse tree, typically the root non-terminal symbol.
     * @return A ParserGrammarTree representing the parsed G4 grammar.
     */
    @Override
    protected ParserGrammarTree createParseTree(NonTerminalNode start) {
        return new ParserGrammarTree(start);
    }

    /**
     * Finalizes and optimizes the parse tree after parsing is complete.
     *
     * This method calls the parent class's `done` method to obtain the raw parse tree, then applies a series of optimizations
     * to streamline the tree structure. These optimizations include removing redundant nodes and simplifying recursive grammar
     * structures, ensuring the final parse tree is both accurate and efficient.
     *
     * @return A fully parsed and optimized ParserGrammarTree.
     */
    @Override
    protected ParserGrammarTree done() {
        ParserGrammarTree tree = super.done();
        this.optimizeNodes(tree.start(), cast(tree.grammarRoot()));

        return tree;
    }

    @Override
    protected void optimizeNodes(NonTerminalNode node, NonTerminalNode root) {
       super.optimizeNodes(node,root);
        NonTerminalNode parent = node.parent();
        //optimize factor
        optimizeFactor(node, root, parent);
        //optimize zero one
        optimizeZeroOne(node, root, parent);
        //optimize zero more
        optimizeZeroMore(node, root, parent);
        //optimize one more
        optimizeOneMore(node, root, parent);
    }

    /**
     * Optimizes nodes representing grammar factors by processing alternative productions.
     *
     * This method restructures parse nodes within a factor, processing alternatives like `OR` and `EPSILON`.
     * It creates new production nodes to handle these alternatives and adds the appropriate child nodes.
     *
     * - If an `OR` node is encountered, it handles splitting productions.
     * - If an epsilon (empty production) is encountered, the corresponding production is optimized.
     *
     * @param node   The current node being processed (factor node).
     * @param root   The root node of the parse tree.
     * @param parent The parent of the current node.
     */
    protected void optimizeFactor(NonTerminalNode node, NonTerminalNode root, NonTerminalNode parent) {
        if (node.name().equals(Constants.FACTOR)) {
            createProduction(node, parent, root, (productions, target, id) -> {
                for (ParseNode n : ((NonTerminalNode) target.nodes().get(1)).nodes()) {
                    if (n.symbol().type() != this.terminals.OR) {//OR terminal node will be ignored
                        if (n.name().equals(Constants.EPSILON) || n.name().equals(Constants.EPSILON_STR)) {
                            epsilonProduction(productions);//create epsilon production
                        } else {
                            productions.addNode(n);//others are all productions
                        }
                    }
                }
            });
        }
    }

    /**
     * Optimizes nodes that represent "zero or one" occurrences in the grammar.
     *
     * The method processes a "zero or one" pattern by creating two alternative productions:
     * one for the epsilon (empty production) case and another for the actual production.
     *
     * @param node   The current node representing the "zero or one" pattern.
     * @param root   The root node of the parse tree.
     * @param parent The parent node of the current node.
     */
    protected void optimizeZeroOne(NonTerminalNode node, NonTerminalNode root, NonTerminalNode parent) {
        if (node.name().equals(Constants.ZERO_ONE)) {
            createProduction(node, parent, root, (productions, target, id) -> {
                epsilonProduction(productions);// zero production
                anotherProduction(productions, target.nodes().get(0));// another production
            });

        }
    }

    /**
     * Optimizes nodes that represent "one or more" occurrences in the grammar.
     *
     * This method creates a production for the "one" case and then wraps it with another production
     * to handle the "zero or more" case recursively.
     *
     * @param node   The current node representing the "one or more" pattern.
     * @param root   The root node of the parse tree.
     * @param parent The parent node of the current node.
     */
    protected void optimizeOneMore(NonTerminalNode node, NonTerminalNode root, NonTerminalNode parent) {
        if (node.name().equals(Constants.ONE_MORE)) {
            createProduction(node, parent, root, (productions, target, id) -> {
                NonTerminalNode production = anotherProduction(productions, target.nodes().get(0));

                NonTerminalNode zeroMore = new NonTerminalNode(new Symbol<>(this.nonTerminals.fromName(Constants.ZERO_MORE)));
                production.addNode(zeroMore);//one more = one + zero more
                zeroMore.addNode(target.nodes().get(0));//this is one
                optimizeZeroMore(zeroMore, root, production);//shi is zero more
            });
        }
    }

    /**
     * Optimizes nodes that represent "zero or more" occurrences in the grammar.
     *
     * This method handles the epsilon (empty) case and then creates an alternative production
     * for the recursive "zero or more" case, wrapping it appropriately in the parse tree structure.
     *
     * @param node   The current node representing the "zero or more" pattern.
     * @param root   The root node of the parse tree.
     * @param parent The parent node of the current node.
     */
    protected void optimizeZeroMore(NonTerminalNode node, NonTerminalNode root, NonTerminalNode parent) {
        if (node.name().equals(Constants.ZERO_MORE)) {
            createProduction(node, parent, root, (productions, target, id) -> {
                epsilonProduction(productions);

                NonTerminalNode pMore = anotherProduction(productions, target.nodes().get(0));
                NonTerminalNode idWrapper = new NonTerminalNode(new Symbol<>(getHead()));
                idWrapper.addNode(id.clone());
                pMore.addNode(idWrapper);
            });
        }
    }

    /**
     * Creates a production node representing an epsilon (empty production).
     *
     * This method adds an epsilon node (which represents an empty production) to the provided productions node.
     * It creates a new non-terminal node for the epsilon production and attaches a terminal node for EPSILON.
     *
     * @param productions The parent node where the epsilon production will be added.
     */
    private void epsilonProduction(NonTerminalNode productions) {
        NonTerminalNode epsilon = new NonTerminalNode(new Symbol<>(this.nonTerminals.fromName(Constants.PRODUCTION)));
        productions.addNode(epsilon);
        epsilon.addNode(new TerminalNode(new Symbol<>(this.terminals.EPSILON)));
    }

    /**
     * Creates a new grammar production and adds it to the parse tree.
     *
     * This method creates the structure of a new grammar production using the specified handler.
     * It defines the head of the grammar, the colon separator, and a block for the production rules,
     * then it adds these elements to the root of the parse tree.
     *
     * The method also updates the parent node by removing the current node and replacing it with
     * the identifier (ID) of the new production. The handler function is called to process the production's body.
     *
     * @param node    The current non-terminal node being processed.
     * @param parent  The parent node of the current non-terminal node.
     * @param root    The root node of the parse tree where the production will be added.
     * @param handler A functional interface to process the production's body, allowing customization.
     */
    private void createProduction(NonTerminalNode node, NonTerminalNode parent, NonTerminalNode root, ParserGrammarParser.TriConsumer<NonTerminalNode, NonTerminalNode, TerminalNode> handler) {

        String name = node.name() + "_" + node.index() + "'";//the created non terminal is an ignored type end with '
        NonTerminalNode grammar = new NonTerminalNode(new Symbol<>(this.nonTerminals.fromName(Constants.GRAMMAR)));
        NonTerminalNode head = new NonTerminalNode(new Symbol<>(getHead()));
        TerminalNode id = new TerminalNode(new Symbol<>(this.terminals.fromName(Constants.ID)));
        id.setToken(new Token(id.symbol().type(), name, node.location()));
        head.addNode(id);
        TerminalNode colon = new TerminalNode(new Symbol<>(this.terminals.COLON, this.terminals.COLON.pattern()));
        NonTerminalNode productions = new NonTerminalNode(new Symbol<>(this.nonTerminals.fromName(Constants.PRODUCTIONS)));

        TerminalNode semicolon = new TerminalNode(new Symbol<>(this.terminals.SEMICOLON, this.terminals.SEMICOLON.pattern()));
        root.addNode(grammar);
        grammar.addNode(head);
        grammar.addNode(colon);
        grammar.addNode(productions);
        handler.accept(productions, node, id);
        grammar.addNode(semicolon);

        NonTerminalNode idWrapper = new NonTerminalNode(new Symbol<>(getHead()));
        idWrapper.addNode(id.clone());
        int index = parent.removeNode(node);
        parent.addNode(idWrapper, index);
    }

    /**
     * Creates and returns a new production node by adding a target node as its child.
     *
     * This method is used to structure new production nodes by adding the target node to them,
     * which allows recursive construction of productions as part of the optimization process.
     *
     * @param productions The parent node where the new production is added.
     * @param target      The target node that will be added to the new production.
     * @return A new non-terminal production node containing the target node.
     */
    private NonTerminalNode anotherProduction(NonTerminalNode productions, ParseNode target) {
        NonTerminalNode pOne = new NonTerminalNode(new Symbol<>(this.nonTerminals.fromName(Constants.PRODUCTION)));
        productions.addNode(pOne);
        pOne.addNode(target);
        return pOne;
    }



    /**
     * A functional interface used to handle customized processing of grammar productions.
     *
     * This interface defines a method `accept` that takes three parameters of types T, U, and V. It is primarily used
     * in the creation and optimization of grammar productions during parsing, allowing flexible handling of different
     * grammar components.
     *
     * @param <T> The type of the first parameter (typically a production node).
     * @param <U> The type of the second parameter (typically a target node).
     * @param <V> The type of the third parameter (typically a terminal node).
     */
    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }


}
