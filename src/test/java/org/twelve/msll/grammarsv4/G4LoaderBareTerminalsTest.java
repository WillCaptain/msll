package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks in PR-L2&#x2032; &mdash; the G4 loader must not seed the user grammar's
 * terminal table with Outline-language built-ins (STRING, ++, ==, COMMA, ...).
 * Before this fix MSLL's built-in {@code STRING} terminal would silently
 * out-compete grammars that declared their own string rule under a different
 * name (focal's {@code STRING_LITERAL}, every grammar that exposes its own
 * comma / dot / operator tokens, ...).
 *
 * <p>The architectural rule we are pinning down here: when a grammar is loaded
 * via {@link G4GrammarLoader}, the only terminals visible to its parser are
 * the structural built-ins (parens, alternation, EOL/END/EPSILON) plus the
 * rules declared in the user's own {@code .g4}. Nothing from
 * {@link Terminals#newMy()} should leak in.
 */
public class G4LoaderBareTerminalsTest {

    /**
     * Outline's STRING built-in has the same length-7 maximal-munch as focal's
     * STRING_LITERAL on input {@code "HELLO"}. Pre-fix the built-in won the
     * tie because it was registered first, the parser saw STRING (which it
     * doesn't know), and predict-table lookup blew up. Post-fix the built-in
     * is not in the table at all, so STRING_LITERAL wins by being the only
     * candidate.
     */
    @Test
    void outline_string_builtin_does_not_pollute_g4_grammar() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "root : STRING_LITERAL ;\n"
                + "STRING_LITERAL : '\"' .*? '\"' ;\n"
                + "WS : [ \\t\\r\\n]+ -> skip ;\n";
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, g4);
        Terminals terms = loaded.builder.terminals();
        assertNull(findByName(terms, "STRING"),
                "Built-in Outline STRING terminal must NOT be present on the bare G4 path. "
                + "Names registered: " + dump(terms));
        assertNotNull(findByName(terms, "STRING_LITERAL"),
                "User-declared STRING_LITERAL must of course be present. Names: " + dump(terms));

        MyParser p = loaded.builder.createParser(new StringReader("\"HELLO\"\n"));
        assertNotNull(p.parse(), "Grammar should parse '\"HELLO\"' as STRING_LITERAL with no built-in interference");
    }

    /**
     * A grammar that declares no string rule at all must lex {@code "abc"}
     * however its own rules say to (or fail loudly). It must NOT silently
     * succeed because the Outline built-in STRING swallowed the token.
     * Here we declare only digits + WS; the string literal should land in
     * the lexer error path, not be silently absorbed.
     */
    @Test
    void grammar_without_string_rule_does_not_pick_up_builtin() throws Exception {
        String g4 = ""
                + "grammar G;\n"
                + "root : INT+ EOF ;\n"
                + "INT : [0-9]+ ;\n"
                + "WS  : [ \\t\\r\\n]+ -> skip ;\n";
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, g4);
        Terminals terms = loaded.builder.terminals();
        for (String banned : Arrays.asList("STRING", "PLUS_PLUS", "EQUAL_EQUAL", "COMMA", "DOT")) {
            assertNull(findByName(terms, banned),
                    "Built-in '" + banned + "' must not appear on a bare G4 path. Names: " + dump(terms));
        }
    }

    private static Terminal findByName(Terminals terms, String name) {
        return terms.fromName(name);
    }

    private static List<String> dump(Terminals terms) {
        return terms.values().stream().map(Terminal::name).toList();
    }
}
