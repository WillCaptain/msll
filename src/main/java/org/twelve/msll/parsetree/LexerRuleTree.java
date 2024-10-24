package org.twelve.msll.parsetree;

import org.twelve.msll.util.Constants;

import java.util.*;

import static org.twelve.msll.util.Tool.cast;

/**
 * The LexerRuleTree represents the parse tree specifically for describing the G4 lexer rules.
 * It is derived from the G4GrammarTree, extending its functionality to handle lexer-specific sections
 * such as the channel definitions and lexer options. This specialized parse tree is designed to capture
 * the detailed configuration and structure of the lexer rules described in G4.
 *
 * The LexerRuleTree also facilitates extracting the 'channels' section, allowing the user to identify and
 * utilize different channels for token grouping within lexer grammar. Channels help categorize tokens for
 * different purposes, which is useful in language parsing scenarios.
 *
 * @author huizi 2024
 */
public class LexerRuleTree extends G4GrammarTree {
    // Stores the list of channels defined in the lexer grammar
    protected List<String> channels = null;

    /**
     * Constructor that initializes the LexerRuleTree with the root NonTerminalNode.
     *
     * @param start The root node of the lexer rule parse tree.
     */
    public LexerRuleTree(NonTerminalNode start) {
        super(start);

    }

    /**
     * Retrieves the head node of the lexer grammar, representing the main lexer definition section.
     *
     * @return an Optional containing the head NonTerminalNode, or an empty Optional if not present.
     */
    public Optional<NonTerminalNode> head() {
        return cast(this.start().nodes().stream().filter(n->n.symbol().type().name().equals(Constants.LEXER_HEAD)).findFirst());
    }

    /**
     * Parses and returns a list of channel names defined in the lexer grammar.
     * If channels have not been parsed yet, this method will initialize the list and extract the channel
     * names from the parse tree.
     *
     * Channels in lexer grammars are used to categorize tokens into different groups, allowing better
     * token management, such as separating tokens for whitespace, comments, and others.
     *
     * @return A list of channel names as Strings.
     */
    public  List<String> channels() {
        if(this.channels==null){
            this.channels = new ArrayList<>();
            //abstract channel
            Optional<NonTerminalNode> channels = cast(cast(this.start().nodes().stream().filter(n->n.symbol().type().name().equals(Constants.CHANNEL_STATEMENT)).findFirst()));
            if (channels.isPresent()) {
                channels.get().nodes().forEach(n -> {
                    if (n.symbol().name().equals(Constants.ID) || n.symbol().name().equals(Constants.UPPER_ID)) {
                        this.channels.add(n.toString());
                    }
                });
            }
        }
        return this.channels;
    }

    /**
     * Returns the name of the lexer grammar represented by this LexerRuleTree.
     *
     * @return The name of the lexer grammar, or "customized language lexer" if not found.
     */
    @Override
    public String name() {
        Optional<NonTerminalNode> head = this.head();
        return head.isPresent() ? head.get().node(1).toString() : "customized language lexer";
    }
}
