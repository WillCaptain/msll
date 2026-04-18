package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Locks in the "synthesise a newline token when the grammar declares one"
 * behaviour (see {@link org.twelve.msll.lexer.RegexLexer#probeNewlineTerminal()}).
 *
 * <p>MSLL's lexer is line-oriented: it flushes the per-line token stream on
 * every {@code \r} / {@code \n}. Grammars that treat newlines as syntactically
 * significant (CSV, properties, INI, ...) must see those newline characters as
 * real tokens; this test guards the shim that makes that work without breaking
 * grammars that do <em>not</em> declare a newline token.
 */
public class NewlineTokenSynthesisTest {

    /** When the grammar declares '\n' as a token, it must reach the parser. */
    @Test
    void newline_literal_in_grammar_reaches_parser() throws Exception {
        String lexer = """
                lexer grammar L;
                NL   : '\\n' ;
                TEXT : [A-Za-z]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : line+ ;
                line : TEXT NL ;
                """;
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(parser), new StringReader(lexer));
        MyParser p = b.createParser(new StringReader("alpha\nbeta\ngamma\n"));
        assertNotNull(p.parse(), "parser should accept three newline-terminated lines");
    }

    /**
     * Grammars that do <em>not</em> declare a newline token must keep their
     * pre-existing line-oriented semantics: the newline is silently dropped,
     * not injected as a spurious token.
     */
    @Test
    void grammar_without_newline_token_is_unchanged() throws Exception {
        String lexer = """
                lexer grammar L;
                WORD : [A-Za-z]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : WORD+ ;
                """;
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(parser), new StringReader(lexer));
        MyParser p = b.createParser(new StringReader("alpha\nbeta\ngamma"));
        assertNotNull(p.parse(),
                "newlines must remain invisible when grammar doesn't declare them");
    }

    /**
     * Sanity check: if the grammar declares newline as a token but the input
     * has no trailing newline, the parser must complain rather than silently
     * accept the incomplete last line.
     */
    @Test
    void missing_trailing_newline_is_rejected_when_required() {
        String lexer = """
                lexer grammar L;
                NL   : '\\n' ;
                TEXT : [A-Za-z]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : (TEXT NL)+ ;
                """;
        assertThrows(Throwable.class, () -> {
            MyParserBuilder b = new MyParserBuilder(
                    new StringReader(parser), new StringReader(lexer));
            MyParser p = b.createParser(new StringReader("alpha"));
            Object tree = p.parse();
            if (tree == null) throw new RuntimeException("null tree");
        });
    }
}
