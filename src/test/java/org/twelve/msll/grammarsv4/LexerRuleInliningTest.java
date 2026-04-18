package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks in MSLL's cross-lexer-rule inlining (L2 in the grammars-v4 compat
 * narrative). Before the fix, a reference from one lexer rule to another
 * (e.g. {@code INTEGER : DIGIT+ ;}) was emitted verbatim &mdash; the compiler
 * fell through to {@code Pattern.quote("DIGIT")}, so {@code INTEGER} matched
 * the literal string {@code "DIGIT"} and never any digits. Only rules that
 * happened to consist of primitive character classes worked.
 *
 * <p>These tests use a minimal grammar built from the same shape as the
 * focal.g4 failure so the probe stays independent of the bigger compat
 * corpus. They also cover a forward reference ({@code INT} before
 * {@code D}) and a chained reference ({@code ID} &rarr; {@code LETTER} and
 * {@code DIGIT}) to make sure the fixed-point iteration resolves both.
 */
public class LexerRuleInliningTest {

    @Test
    void non_fragment_rule_is_inlined_into_referring_rule() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "root : INTEGER ;\n"
                + "INTEGER : DIGIT+ ;\n"
                + "DIGIT   : [0-9] ;\n"
                + "WS      : [ \\t\\r\\n]+ -> skip ;\n";
        Terminals terms = terminalsFor(g4);
        Terminal integer = findByName(terms, "INTEGER");
        assertNotNull(integer, "INTEGER terminal should be registered");
        Matcher m = integer.compiledPattern().matcher("42");
        assertTrue(m.lookingAt(),
                "INTEGER should now expand to [0-9]+ via cross-rule inlining, "
                + "not to the literal string 'DIGIT'. compiled=" + integer.compiledPattern().pattern());
        assertEquals(2, m.end(), "INTEGER must match both digits greedily");
    }

    @Test
    void forward_reference_resolves_through_fixed_point_iteration() throws Exception {
        // INTEGER is declared BEFORE DIGIT – the classic focal.g4 shape.
        // The iterative inlining pass must still resolve the forward reference.
        String g4 = ""
                + "grammar G;\n"
                + "root : INTEGER ;\n"
                + "INTEGER : D+ ;\n"
                + "D       : [0-9] ;\n"
                + "WS      : [ \\t\\r\\n]+ -> skip ;\n";
        Terminals terms = terminalsFor(g4);
        Terminal integer = findByName(terms, "INTEGER");
        assertNotNull(integer);
        assertTrue(integer.compiledPattern().matcher("7").lookingAt(),
                "INTEGER should match digits even when the referenced rule D is declared later");
    }

    @Test
    void chained_references_are_transitively_expanded() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "root : ID ;\n"
                + "ID     : LETTER (LETTER | DIGIT)* ;\n"
                + "LETTER : [A-Za-z] ;\n"
                + "DIGIT  : [0-9] ;\n"
                + "WS     : [ \\t\\r\\n]+ -> skip ;\n";
        Terminals terms = terminalsFor(g4);
        Terminal id = findByName(terms, "ID");
        assertNotNull(id);
        Matcher m = id.compiledPattern().matcher("abc123");
        assertTrue(m.lookingAt(), "ID = LETTER (LETTER|DIGIT)* should match 'abc123'");
        assertEquals(6, m.end(), "ID should consume the full 'abc123'");
    }

    // --- helpers ---

    private static Terminals terminalsFor(String combinedG4) throws Exception {
        // Bare loader path; Terminals/NonTerminals exposed via the public accessor
        // on ParserBuilder (the bare builder uses Terminals.newBare()).
        return G4GrammarLoader.loadG4String(null, combinedG4).builder.terminals();
    }

    @SuppressWarnings("unchecked")
    private static Terminal findByName(Terminals terms, String name) throws Exception {
        Field listF = Terminals.class.getDeclaredField("terminals");
        listF.setAccessible(true);
        List<Terminal> all = (List<Terminal>) listF.get(terms);
        for (Terminal t : all) if (t.name().equals(name)) return t;
        return null;
    }
}
