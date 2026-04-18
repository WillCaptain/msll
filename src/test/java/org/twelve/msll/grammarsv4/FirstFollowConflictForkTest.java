package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parser.PredictTable;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks in the auto-detected FIRST/FOLLOW conflict handling (L1 in the
 * grammars-v4 compat narrative) that compensates for MSLL's fixed-length
 * lookahead.
 *
 * <p>The canonical shape is a Kleene closure whose continuation token is
 * also in FOLLOW of the closure itself: the parser cannot decide between
 * "take another iteration" and "stop here" from one token of lookahead
 * alone. ANTLR4 solves this with adaptive LL*; MSLL instead asks the
 * multi-stack runtime to fork both alternatives and lets whichever one
 * successfully consumes the rest of the input win.
 *
 * <p>Before the fix this only worked for a hand-maintained whitelist
 * ({@code MyParser.epsilonAlongsideGrammars}); now every conflicted cell
 * in a G4-loaded grammar is registered automatically at
 * {@link PredictTable} construction time and the loader opts in.
 */
public class FirstFollowConflictForkTest {

    /**
     * ABNF-shaped minimal reproducer: {@code rule_*} where every rule starts
     * with {@code ID}, and the body of a rule can also end in {@code ID}.
     * Without fork-on-conflict, the parser greedily absorbs the next
     * {@code ID} into the current rule's body and then trips on the
     * following {@code '='}.
     */
    @Test
    void kleene_closure_with_overlapping_first_and_follow_sets() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "prog   : rule_* EOF ;\n"
                + "rule_  : ID '=' body ;\n"
                + "body   : atom+ ;\n"
                + "atom   : ID | STRING ;\n"
                + "ID     : [A-Za-z] [A-Za-z0-9]* ;\n"
                + "STRING : '\"' ~'\"'* '\"' ;\n"
                + "WS     : [ \\t\\r\\n]+ -> skip ;\n";
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, g4);
        // Two rules where the first rule's `body` ends in ID and the second
        // rule also starts with ID – exactly the ABNF failure shape.
        MyParser p = loaded.builder.createParser(new StringReader(
                "ALPHA = FOO\nBETA  = \"b\"\n"));
        assertNotNull(p.parse(),
                "Parser must fork 'atom+' on ID: one fork continues the body, "
                + "the other stops so the next ID starts rule_ #2.");
    }

    @Test
    void auto_detection_discovers_the_conflict_cell() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "prog  : rule_* EOF ;\n"
                + "rule_ : ID '=' atom+ ;\n"
                + "atom  : ID ;\n"
                + "ID    : [A-Za-z]+ ;\n"
                + "WS    : [ \\t\\r\\n]+ -> skip ;\n";
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, g4);
        PredictTable table = loaded.builder.predictTable();
        // The closure over atom is compiled into an auxiliary non-terminal
        // (conventionally named with a trailing "'"). That cell should hold
        // both an epsilon and a non-epsilon production under ID.
        boolean found = table.autoEpsilonAlongsideCells().entrySet().stream()
                .anyMatch(e -> e.getValue().contains("ID"));
        assertTrue(found, "Auto-detection should flag at least one FIRST/FOLLOW conflict on ID, "
                + "got: " + table.autoEpsilonAlongsideCells());
    }
}
