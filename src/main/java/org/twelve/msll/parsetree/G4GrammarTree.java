package org.twelve.msll.parsetree;

import org.twelve.msll.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.twelve.msll.util.Tool.cast;

/**
 * The G4GrammarTree represents the parse tree structure for describing the parser grammar and lexer rule of G4 files.
 * This class is an extension of the standard ParserTree and includes specific handling of special sections in G4,
 * such as the 'options' section. This section defines configuration settings that influence the behavior of the grammar.
 *
 * Although this class encapsulates most of the core G4 grammar tree functionality, there are some unfinished components
 * that may require further implementation.
 *
 * Additional work has been done in related classes like the ParserTreeGrammarBuilder, which is responsible for building
 * this tree from the parsed G4 source. The builder handles essential processes such as constructing the terminals,
 * non-terminals, and productions based on the G4 grammar structure.
 *
 * It has two derived classes: ParserGrammarTree for parser grammars and LexerRuleTree for lexer rules
 *
 * @author huizi 2024
 */
public abstract class G4GrammarTree extends ParserTree {

    // Stores the configuration options parsed from the 'options' section of the G4 file
    protected Map<String, String> options = null;

    /**
     * Constructor that initializes the G4GrammarTree with the root NonTerminalNode.
     *
     * @param start The root node of the G4 grammar parse tree.
     */
    public G4GrammarTree(NonTerminalNode start) {
        super(start);

    }

    /**
     * Retrieves the head node of the grammar, representing the starting point of the G4 grammar definition.
     *
     * @return an Optional containing the head NonTerminalNode, or an empty Optional if not present.
     */
    public abstract Optional<NonTerminalNode> head();

    /**
     * Returns the name of the grammar represented by this G4GrammarTree.
     *
     * @return The name of the grammar.
     */
    public abstract String name();

    /**
     * Extracts the root node for the grammar section in the parse tree. This node contains the set of grammars
     * defined in the G4 file and serves as the foundation for further processing and validation.
     *
     * @return The NonTerminalNode representing the grammar root.
     */
    public NonTerminalNode grammarRoot() {
        return cast(this.start().nodes().stream().filter(n->n.symbol().type().name().equals(Constants.GRAMMARS)).findFirst().get());
    }

    /**
     * Parses and returns the map of configuration options defined in the 'options' section of the G4 grammar.
     * If options have not been parsed yet, this method will initialize the map and extract the options from
     * the parse tree.
     *
     * @return A map of option names to their corresponding values.
     */
    public Map<String, String> options(){
        if(this.options==null){
            this.options = new HashMap<>();
            //abstract options
            Optional<NonTerminalNode> options = cast(this.start().nodes().stream().filter(n->n.symbol().type().name().equals(Constants.OPTIONS_STATEMENT)).findFirst());;
            if (options.isPresent()) {
                options.get().nodes().forEach(n -> {
                    if (!n.symbol().name().equals(Constants.OPTION)) return;
                    NonTerminalNode option = cast(n);
                    this.options.put(option.node(0).toString(), option.node(2).toString());
                });
            }
        }
        return this.options;
    }
}
