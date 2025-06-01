package org.twelve.msll.parsetree;

import lombok.Setter;
import org.twelve.msll.grammarsymbol.NonTerminal;
import org.twelve.msll.lexer.Location;
import org.twelve.msll.parser.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * A `NonTerminalNode` represents a non-terminal symbol in the parse tree.
 * It is a specific type of `ParseNode` that is used to store non-terminal nodes,
 * such as grammar rules that are further expandable into other rules or terminals.
 * <p>
 * In contrast to `TerminalNode`, which contains only one terminal token, a `NonTerminalNode`
 * typically contains a list of other `ParseNode`s (both terminals and non-terminals),
 * which correspond to the production rule being expanded.
 * <p>
 * The `NonTerminalNode` is central to forming the structure of the parse tree,
 * as it can recursively hold other nodes and represent complex grammar rules.
 *
 * @author huizi 2024
 */
public class NonTerminalNode extends ParseNode<NonTerminal> {
    // List of child nodes (both Terminal and NonTerminal nodes)
    private final List<ParseNode> nodes = new ArrayList<>();
    @Setter
    private String explain;

    /**
     * Constructs a `NonTerminalNode` with the given non-terminal symbol.
     *
     * @param symbol The non-terminal symbol this node represents.
     */
    public NonTerminalNode(Symbol<NonTerminal> symbol) {
        super(symbol);
    }

    public String explain(){
        return this.explain;
    }

    /**
     * Returns the location in the source code corresponding to this non-terminal node.
     * The location is based on the first and last child nodes.
     *
     * @return The location of the node in the source code.
     */
    @Override
    public Location location() {
        return new Location(this.nodes.get(0).location().start(), this.nodes.get(this.nodes.size() - 1).location().end(), this.nodes.get(0).location().line());
    }

    /**
     * Checks if this node is the start node of the parse tree.
     *
     * @return True if this is the start node, otherwise false.
     */
    public boolean isStart() {
        return this.symbol.type().isStart();
    }

    /**
     * Adds a child node to this non-terminal node at the specified index.
     * If the index is -1, the node is added to the end.
     *
     * @param child The child node to add.
     * @param index The position where the node should be inserted.
     */
    public void addNode(ParseNode child, int index) {
        child.setParent(this);
        if (child.parseTree() != this.parserTree) {
            child.setParseTree(this.parserTree);
        }
        if (index == -1) {
            this.nodes.add(child);
        } else {
            this.nodes.add(index, child);
        }
    }

    /**
     * Adds a child node to the end of this non-terminal node.
     *
     * @param child The child node to add.
     */
    public void addNode(ParseNode child) {
        addNode(child, -1);
    }

    /**
     * Sets the parse tree for this non-terminal node and propagates it to all child nodes.
     *
     * @param parserTree The parser tree this node belongs to.
     */
    @Override
    public void setParseTree(ParserTree parserTree) {
        super.setParseTree(parserTree);
        for (ParseNode node : this.nodes) {
            node.setParseTree(parserTree);
        }
    }

    /**
     * Returns the non-terminal symbol associated with this node.
     *
     * @return The non-terminal symbol.
     */
    public NonTerminal nonTerminal() {
        return this.symbol.type();
    }

    /**
     * Removes the specified child node and returns its index in the list.
     *
     * @param node The node to remove.
     * @return The index of the removed node.
     */
    public int removeNode(ParseNode node) {
        int index = this.indexOf(node);
        this.nodes.remove(node);
        node.setParent(null);
        return index;
    }

    /**
     * Returns a list of all child nodes under this non-terminal node.
     *
     * @return A list of child nodes.
     */
    public List<ParseNode> nodes() {
        return new ArrayList<>(this.nodes);
    }

    /**
     * Returns the child node at the specified index.
     *
     * @param index The index of the child node.
     * @return The child node at the specified index.
     */
    public ParseNode node(int index) {
        return this.nodes().get(index);
    }

    /**
     * Adds a list of nodes to then end of this non-terminal node.
     *
     * @param nodes The list of nodes to add.
     */
    public void addNodes(List<ParseNode> nodes) {
        for (ParseNode node : nodes) {
            this.addNode(node);
        }
    }

    /**
     * Adds a list of nodes to this non-terminal node at the specified index.
     *
     * @param nodes The list of nodes to add.
     * @param index The position to insert the nodes.
     */
    public void addNodes(List<ParseNode> nodes, int index) {
        int i = index;
        for (ParseNode node : nodes) {
            this.addNode(node, i++);
        }
    }

    /**
     * Returns the index of the specified node within this non-terminal's child nodes.
     *
     * @param node The node to search for.
     * @return The index of the node, or -1 if not found.
     */
    public int indexOf(ParseNode node) {
        return this.nodes.indexOf(node);
    }

    @Override
    public String toString() {
        if (this.nodes.size() == 0) return this.symbol.name();
        StringBuilder display = new StringBuilder();//this.nodes.size()>1?super.toString():"";
        for (ParseNode node : this.nodes) {
            display.append(node + " ");
        }
        return display.toString();
    }

    /**
     * Returns the lexeme (actual value) of the non-terminal and its children as a string.
     *
     * @return The concatenated lexeme of the node.
     */
    @Override
    public String lexeme() {
        return this.toString().replace(" ", "");
    }


}
