package org.twelve.msll.parser;

import org.twelve.msll.grammar.Grammar;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammar.Production;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.util.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * (grammarName -> {terminalName,...}) cells where an &epsilon;-producing
     * production coexists with a non-&epsilon; production in the same cell.
     *
     * <p>These are the LL(1) FIRST/FOLLOW conflicts MSLL resolves by forking
     * the parse stack: one fork takes the non-&epsilon; path, the other takes
     * &epsilon; (effectively "stop here and bubble up"). Whichever fork parses
     * the rest of the input wins; the other dies on a grammar exception and is
     * pruned. This is MSLL's runtime analogue of ANTLR4's adaptive LL*.
     *
     * <p>Computed once at table construction time (see {@link #detectConflicts()}),
     * read by {@link MsllParser#matchNonTerminalToken} to decide whether to keep
     * epsilon alongside.
     */
    private final Map<String, Set<String>> epsilonAlongsideCells = new HashMap<>();

    /**
     * When {@code false} (default), {@link #hasEpsilonAlongside} always returns
     * {@code false} and the parser relies solely on the hand-curated whitelist
     * in {@link MsllParser#epsilonAlongsideGrammars}. This preserves byte-exact
     * behaviour for every grammar that existed before auto-conflict detection
     * landed. The G4 loader path flips this on to get ANTLR4-style semantics.
     */
    private boolean autoEpsilonAlongsideEnabled = false;

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
        detectConflicts();
    }

    /**
     * Walks the finished table and records every (grammar, terminal) cell that
     * contains <em>both</em> an empty and a non-empty production. Those cells
     * are the classic LL(1) FIRST/FOLLOW conflict points where a Kleene closure
     * (X*, X?, (...)?) could legitimately end <em>or</em> continue on the same
     * token &mdash; for example ABNF's {@code rule_*} where FIRST(rule_) and
     * FOLLOW(rulelist) both contain {@code ID}.
     *
     * <p>Conflict cells are exposed via {@link #hasEpsilonAlongside(String, String)}
     * so the runtime can fork the parse stack instead of silently preferring the
     * non-empty branch.
     */
    private void detectConflicts() {
        for (Map.Entry<Terminal, Map<Grammar, List<Production>>> byTerm : table.entrySet()) {
            String terminalName = byTerm.getKey().name();
            for (Map.Entry<Grammar, List<Production>> byGrammar : byTerm.getValue().entrySet()) {
                List<Production> ps = byGrammar.getValue();
                if (ps.size() < 2) continue;
                boolean hasEmpty = false, hasNonEmpty = false;
                for (Production p : ps) {
                    if (p == null) continue;
                    if (p.isEmpty()) hasEmpty = true; else hasNonEmpty = true;
                }
                if (hasEmpty && hasNonEmpty) {
                    epsilonAlongsideCells
                            .computeIfAbsent(byGrammar.getKey().name(), k -> new HashSet<>())
                            .add(terminalName);
                }
            }
        }
    }

    /**
     * @return true when {@code (grammarName, terminalName)} is a FIRST/FOLLOW
     * conflict cell and the runtime should keep &epsilon; productions alongside
     * non-&epsilon; ones rather than filtering them out.
     */
    public boolean hasEpsilonAlongside(String grammarName, String terminalName) {
        if (!autoEpsilonAlongsideEnabled) return false;
        Set<String> terms = epsilonAlongsideCells.get(grammarName);
        return terms != null && terms.contains(terminalName);
    }

    /**
     * Opt-in switch for the auto-detected {@link #epsilonAlongsideCells}. Leave
     * unset for legacy MSLL grammars (unchanged behaviour); flip on for G4-loaded
     * grammars, where MSLL is aiming to mimic ANTLR4's adaptive lookahead.
     */
    public void setAutoEpsilonAlongsideEnabled(boolean enabled) {
        this.autoEpsilonAlongsideEnabled = enabled;
    }

    /**
     * Exposed for tests / diagnostics: the full set of auto-detected conflict
     * cells, regardless of whether auto-mode is enabled.
     */
    public Map<String, Set<String>> autoEpsilonAlongsideCells() {
        return epsilonAlongsideCells;
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
            StringBuilder sb = new StringBuilder();
            for (List<Production> ps : grammars.values()) {
                for (Production p : ps) {
                    sb.append(p).append(lineSeparator);
                }
            }
            String keywordHint = !terminal.isRegex()
                    ? lineSeparator + "Hint: '" + token.lexeme() + "' is a reserved keyword and cannot be used as an identifier or in this position."
                    : "";
            Tool.grammarError("token: " + terminal.name() + ":" + token.lexeme()
                    + " doesn't match any grammar definition at line: " + token.location().line().number()
                    + ", position from " + (token.location().start() - token.location().line().beginIndex()) + " to "
                    + (token.location().end() - token.location().line().beginIndex()) + lineSeparator + line
                    + keywordHint
                    + lineSeparator + " the possible productions should be matched would be:" + sb);
        }
        return productions;
    }
}
