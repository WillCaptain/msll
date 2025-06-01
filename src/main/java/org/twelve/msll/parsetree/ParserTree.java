package org.twelve.msll.parsetree;

import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.util.Tool;

import static org.twelve.msll.util.Tool.cast;

/**
 * Represents the parse tree, which is the result of parsing an input file using a self-defined grammar.
 * <p>
 * A `ParserTree` holds the hierarchical structure of the parsed input, consisting of nodes that represent
 * both terminals (tokens) and non-terminals (grammar rules). It is built incrementally during the parsing
 * process and can be further polished to remove redundant information.
 *
 * @author huizi 2024
 */
public class ParserTree {
    /**
     * The root node of the parse tree, which is always a non-terminal node.
     * in parser tree, a root node is required
     */
    private final NonTerminalNode start;

    /**
     * Constructs a `ParserTree` with the specified root node.
     * The root node must be a valid start symbol, typically defined in the grammar as the entry point.
     *
     * @param start The root node of the parse tree.
     */
    public ParserTree(NonTerminalNode start) {
        if (!start.isStart()) Tool.grammarError("root node of parse tree must be a start node");
        this.start = start;
        this.start.setParseTree(this);
    }

    /**
     * Returns the root node of the parse tree.
     *
     * @return The root node, which is a non-terminal.
     */
    public NonTerminalNode start() {
        return this.start;
    }

    /**
     * Polishes the parse tree to remove redundant information generated during parsing.
     * This includes:
     * - Clearing flags associated with expired stacks (from failed parse paths).
     * - Removing nodes that are marked as ignored.
     * - Eliminating empty non-terminal nodes that do not contribute to the final structure.
     */
    public void polish() {
        this.clearFlags(this.start);
        this.clearIgnores(this.start, null);
        this.clearEmptyNonTerminals(this.start);
    }

    /**
     * Removes empty non-terminal nodes that were created during parsing but do not hold any child nodes.
     *
     * @param node The current non-terminal node.
     */
    private void clearEmptyNonTerminals(NonTerminalNode node) {
        if (node.nodes().size() == 0) {
            node.parent.removeNode(node);
        } else {
            for (ParseNode n : node.nodes()) {
                if (n instanceof NonTerminalNode) {
                    clearEmptyNonTerminals(cast(n));
                }
            }
        }
    }

    /**
     * Clears nodes that have expired flags, which indicate that the path associated with those nodes failed
     * in the multi-stack parsing process.
     *
     * @param node The current parse node.
     */
    private void clearFlags(ParseNode node) {
        if (node.flag().expired()) {
            node.parent().removeNode(node);
        } else {
            if (node instanceof NonTerminalNode)
                for (ParseNode child : ((NonTerminalNode) node).nodes()) {
                    clearFlags(child);
                }
        }
    }

    /**
     * Removes intermediate non-terminal nodes that are marked as ignored. These nodes typically come from
     * recursive or layered grammar structures e.g.,
     * statements -> statement statements'
     * statements' -> epsilon | statement statements'
     * statements' with ' end is an ignored node
     *
     * @param node   The current non-terminal node.
     * @param parent The parent node of the current node.
     */
    private void clearIgnores(NonTerminalNode node, NonTerminalNode parent) {
        for (ParseNode child : node.nodes()) {
            if (child instanceof NonTerminalNode) {
                clearIgnores(cast(child), node);
            }
        }
        if (node.symbol().type() == NonTerminals.IGNORED) {
            if(node.explain()!=null && !node.explain().isEmpty()) {
                return;
            }
            int index = parent.removeNode(node);
            parent.addNodes(node.nodes(), index);
        }
    }


}
