package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Layered probes for the lexer features JSON's grammar depends on.
 *
 * <p>JSON fails end-to-end with {@code "something wrong in lexing"}. Rather
 * than fight the full grammar, each test here isolates one G4-style lexer
 * construct so a failure tells us <em>exactly</em> which compilation path is
 * broken:
 *
 * <ol>
 *   <li>Fragment composition in a non-string rule.</li>
 *   <li>Negated-char-class with inline quantifier.</li>
 *   <li>A negated class containing an embedded Unicode range
 *       &mdash; this is exactly JSON's {@code SAFECODEPOINT}.</li>
 *   <li>The full JSON {@code STRING} rule that combines all of the above.</li>
 * </ol>
 *
 * <p>Each probe uses hand-written {@code .gm} source (no conversion step) so
 * we stay inside the MSLL engine and rule out {@link org.twelve.msll.tools.G4GrammarLoader}
 * as the culprit.
 */
public class LexerFeatureProbeTest {

    /** Builds a parser for (lexerGm, parserGm) and parses {@code input}. */
    private static void parse(String lexerGm, String parserGm, String input) throws Exception {
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(parserGm), new StringReader(lexerGm));
        MyParser p = b.createParser(new StringReader(input));
        assertNotNull(p.parse(), "parse tree was null for input: " + input);
    }

    /** Fragments compose as expected: INT references DIGIT. */
    @Test
    void fragment_composition_in_lexer_rule() throws Exception {
        String lexer = """
                lexer grammar L;
                fragment DIGIT : [0-9] ;
                INT : DIGIT+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : INT ;
                """;
        parse(lexer, parser, "1234");
    }

    /** Simple negated char class with quantifier: matches everything except '"'. */
    @Test
    void negated_char_class_with_quantifier() throws Exception {
        String lexer = """
                lexer grammar L;
                NONQ : ~["]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : NONQ ;
                """;
        parse(lexer, parser, "hello");
    }

    /**
     * Negated class containing an embedded Unicode range — exactly the form
     * JSON uses for {@code SAFECODEPOINT : ~["\\\\\u0000-\u001F]}.
     */
    @Test
    void negated_char_class_with_unicode_range() throws Exception {
        String lexer = """
                lexer grammar L;
                SAFE : ~["\\\\\\u0000-\\u001F]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : SAFE ;
                """;
        parse(lexer, parser, "abc");
    }

    /** The full JSON STRING rule, isolated. */
    @Test
    void json_string_rule_in_isolation() throws Exception {
        String lexer = """
                lexer grammar L;
                STRING : '"' (ESC | SAFECODEPOINT)* '"' ;
                fragment ESC : '\\\\' (["\\\\/bfnrt] | UNICODE) ;
                fragment UNICODE : 'u' HEX HEX HEX HEX ;
                fragment HEX : [0-9a-fA-F] ;
                fragment SAFECODEPOINT : ~["\\\\\\u0000-\\u001F] ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : STRING ;
                """;
        parse(lexer, parser, "\"hello world\"");
    }
}
