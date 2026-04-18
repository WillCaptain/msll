package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.tools.G4GrammarLoader;
import org.twelve.msll.tools.G4Splitter;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L3 regression: ANTLR4 lexer modes.
 *
 * <p>Exercises the machinery that makes grammars like {@code properties.g4}
 * and {@code ini.g4} parseable:
 * <ul>
 *   <li>{@link G4Splitter} preserves {@code mode X;} declarations even when
 *       they appear between lexer rules.</li>
 *   <li>Per-rule mode tagging flows into each {@link Terminal}.</li>
 *   <li>{@code Terminals.addSymbol} does <b>not</b> dedup across different
 *       modes: {@code NL : '\n'} (DEFAULT_MODE) and
 *       {@code NL_VAL : '\n' -> type(NL), popMode} (mode VAL) coexist even
 *       though they share a pattern.</li>
 *   <li>The {@code -> pushMode / popMode / type} lexer commands are carried
 *       on the terminal.</li>
 *   <li>End-to-end: a KEY/VALUE grammar where KEY and VALUE overlap on
 *       identifier-like text succeeds because the SEP token flips modes.</li>
 * </ul>
 */
public class LexerModeTest {

    private static final String G4 =
              "grammar kv;\n"
            + "root : line+ EOF ;\n"
            + "line : KEY SEP VALUE NL ;\n"
            + "KEY  : [A-Za-z_] [A-Za-z_0-9]* ;\n"
            + "SEP  : '=' -> pushMode(VAL) ;\n"
            + "NL   : '\\n' ;\n"
            + "WS   : [ \\t]+ -> skip ;\n"
            + "mode VAL;\n"
            + "VALUE  : ~[\\r\\n]+ -> popMode ;\n"
            + "NL_VAL : '\\n' -> type(NL), popMode ;\n";

    @Test
    void splitter_keeps_mode_decl_between_rules() {
        G4Splitter.Split split = G4Splitter.split(G4, null);
        assertEquals(G4Splitter.Form.COMBINED, split.form);
        assertTrue(split.lexer.contains("mode VAL;"),
                "mode declaration must end up in the lexer half, got:\n" + split.lexer);
        assertFalse(split.parser.contains("mode VAL;"),
                "mode declaration must NOT leak into parser half");
    }

    @Test
    void terminals_carry_mode_and_lexer_command() throws Exception {
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, G4);
        Terminal key = loaded.builder.terminals().fromName("KEY");
        Terminal sep = loaded.builder.terminals().fromName("SEP");
        Terminal value = loaded.builder.terminals().fromName("VALUE");
        Terminal nl = loaded.builder.terminals().fromName("NL");
        Terminal nlVal = loaded.builder.terminals().fromName("NL_VAL");

        assertEquals("DEFAULT_MODE", key.mode());
        assertEquals("DEFAULT_MODE", sep.mode());
        assertEquals("VAL", value.mode());
        assertEquals("DEFAULT_MODE", nl.mode(), "NL must stay in DEFAULT_MODE");
        assertEquals("VAL", nlVal.mode(),
                "NL_VAL is a distinct terminal (same pattern, different mode)");
        assertTrue(sep.getCommand() != null && sep.getCommand().contains("pushMode(VAL)"),
                "SEP must carry its pushMode command");
        assertTrue(value.getCommand() != null && value.getCommand().contains("popMode"));
    }

    @Test
    void parser_succeeds_when_key_and_value_overlap_lexically() throws Exception {
        // "hello" matches both KEY and VALUE. Without modes the lexer's
        // maximal-munch tie-breaker picks KEY on both sides of '=', so the
        // parser sees KEY SEP KEY instead of KEY SEP VALUE and fails.
        // With modes, SEP flips to VAL-only, VALUE wins the RHS.
        G4GrammarLoader.Loaded loaded = G4GrammarLoader.loadG4String(null, G4);
        MyParser p = loaded.builder.createParser(new StringReader("host=hello\nport=world\n"));
        assertDoesNotThrow(p::parse);
    }
}
