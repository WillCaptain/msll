package org.twelve.msll.parsetree;

import org.twelve.msll.util.Constants;

import java.util.Optional;

import static org.twelve.msll.util.Tool.cast;

/**
 * The ParserGrammarTree represents the parse tree specifically for describing the G4 parser grammar rules.
 * This class serves a role similar to the LexerRuleTree, but instead of handling lexer rules, it handles
 * parser rules within G4 files. It extends G4GrammarTree, providing specialized methods to interpret
 * parser grammar components and extract key details, such as the grammar head and its name.
 *
 * The ParserGrammarTree is designed to handle the specific syntax for defining parser rules, including
 * extracting grammar-related options and metadata. It works in conjunction with other derived classes
 * (e.g., LexerRuleTree) to provide a complete representation of G4 grammars, enabling parsing and interpreting
 * both lexer and parser grammar segments.
 *
 * This class is used when interpreting and converting G4 parser grammar definitions into CFG format, which
 * then can be utilized for building further customized language parsers.
 *
 * @author huizi 2024
 */
public class ParserGrammarTree extends G4GrammarTree {

    /**
     * Constructor that initializes the ParserGrammarTree with the root NonTerminalNode.
     *
     * @param start The root node of the parser grammar parse tree.
     */
    public ParserGrammarTree(NonTerminalNode start) {
        super(start);
    }

    /**
     * Retrieves the head node of the parser grammar, representing the main parser definition section.
     *
     * @return an Optional containing the head NonTerminalNode, or an empty Optional if not present.
     */
    public Optional<NonTerminalNode> head() {
        return cast(this.start().nodes().stream().filter(n->n.symbol().type().name().equals(Constants.PARSER_HEAD)).findFirst());
    }

    /**
     * Returns the name of the parser grammar represented by this ParserGrammarTree.
     *
     * @return The name of the parser grammar, or "customized language parser" if the name cannot be found.
     */
    @Override
    public String name() {
        Optional<NonTerminalNode> head = this.head();
        return head.isPresent() ? head.get().node(1).toString() : "customized language parser";
    }

}
