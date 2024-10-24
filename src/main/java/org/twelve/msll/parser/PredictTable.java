package org.twelve.msll.parser;

import org.twelve.msll.grammar.Grammar;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammar.Production;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.util.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PredictTable represents the prediction table used in MSLL parsing, closely resembling the traditional LL(1)
 * prediction table with two key differences:
 * 1. There is no need to handle first-first conflicts when building the table, allowing for overlapping production rules.
 * 2. During parsing, the prediction table can match more than one production for a given token, supporting dynamic multi-path exploration.
 *
 * The `PredictTable` maps terminals to possible productions in the grammar, and during parsing, it provides
 * the possible set of productions for each token, which the parser will use to proceed along multiple paths when necessary.
 *
 * @author huizi 2024
 */
public class PredictTable {
    // The prediction table is represented as a map where each terminal maps to a grammar's list of possible productions.
    private final Map<Terminal, Map<Grammar, List<Production>>> table = new HashMap<>();

    /**
     * Builds the prediction table by populating it with grammar rules and their respective FIRST and FOLLOW sets.
     *
     * For each grammar, the method computes the FIRST and FOLLOW sets and adds the corresponding terminal symbols
     * to the table. If a production can produce an empty (ε) symbol, the FOLLOW set is added.
     *
     * @param grammars The set of grammars for which the prediction table is built.
     */
    public PredictTable(Grammars grammars) {
        grammars.grammars().forEach(grammar -> {
            AtomicBoolean hasEmpty = new AtomicBoolean(false);
            grammar.productions().forEach(production -> {
                if (production.isEmpty()) {
                    // For empty productions, use the FOLLOW set to predict
                    grammar.follow().forEach(symbol -> addMapping(grammar, production, symbol));
                    hasEmpty.set(true);
                }
                // Add terminal symbols from the FIRST set to the prediction table
                production.first().forEach(symbol -> addMapping(grammar, production, symbol));
            });
            // For grammars that can produce epsilon indirectly, use the FOLLOW set
            if (!hasEmpty.get() && grammar.containsEmptyFirst()) {
                grammar.follow().forEach(symbol -> addMapping(grammar, null, symbol));
            }
        });
    }

    /**
     *  Helper method to add mappings of a grammar, production, and terminal to the prediction table.
     */
    private void addMapping(Grammar grammar, Production production, Terminal symbol) {
        Map<Grammar, List<Production>> firsts = table.computeIfAbsent(symbol, k -> new HashMap<>());
        List<Production> productions = firsts.get(grammar);
        if (productions == null) {
            productions = new ArrayList<>();
            firsts.put(grammar, productions);
        }
        productions.add(production);
    }

    /**
     * Matches the current token to a set of productions based on the given grammar.
     *
     * The method searches the prediction table for the possible productions associated with the current token.
     * If no match is found, an error is raised, and the possible matching productions are displayed for debugging purposes.
     *
     * @param token The current token being parsed.
     * @param grammar The grammar for which to find the matching productions.
     * @param line The current line being parsed for error reporting.
     * @return The list of productions that match the current token.
     */
    public List<Production> match(Token token, Grammar grammar, String line) {
        Terminal terminal = token.terminal();
        Map<Grammar, List<Production>> grammars = table.get(terminal);
        String lineSeparator = System.lineSeparator();
        if (grammars == null) {
            Tool.grammarError(
                    "`" + terminal.pattern() + "` is not found in predict table" + lineSeparator + line);
        }
        List<Production> productions = grammars.get(grammar);
        if (productions == null) {
//        if (productions.size() == 0) {
            StringBuilder sb = new StringBuilder();
            for (List<Production> ps : grammars.values()) {
                for (Production p : ps) {
                    sb.append(p).append(lineSeparator);
                }
            }
            Tool.grammarError("token: " + terminal.name() + ":" + token.lexeme()
                    + " doesn't match any ohScript grammar definition at line: " + token.location().line().number()
                    + ", position from " + (token.location().start() - token.location().line().beginIndex()) + " to "
                    + (token.location().end() - token.location().line().beginIndex()) + lineSeparator + line
                    + lineSeparator + " the possible productions should be matched would be:" + sb);
        }
        return productions;
    }
}
