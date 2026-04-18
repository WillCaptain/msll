package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.ParserTree;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sanity probe: proves whether a hand-written {@code LIT_3A : ':' ;} style
 * lexer rule actually produces a {@code LIT_3A} terminal at runtime.
 *
 * <p>Used to disentangle lifter bugs (bad grammar emission) from MSLL
 * runtime bugs (single-char literal tokens not being registered).
 */
public class ImplicitTokenProbeTest {

    private static final String LEXER = """
            lexer grammar Probe;

            LIT_3A : ':' ;
            IDENT  : [a-z]+ ;
            """;

    private static final String PARSER = """
            parser grammar ProbeParser;

            options {
                tokenVocab = Probe;
            }

            root : IDENT LIT_3A IDENT ;
            """;

    @Test
    void colon_literal_token_is_recognised() throws Exception {
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(PARSER), new StringReader(LEXER));
        MyParser p = b.createParser(new StringReader("foo:bar"));
        ParserTree tree = p.parse();
        assertNotNull(tree, "parse tree should not be null");
    }
}
