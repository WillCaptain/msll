package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Locks in generalised multi-line token support
 * (see {@link org.twelve.msll.lexer.CodeCache}).
 *
 * <p>MSLL's lexer historically treated source as a sequence of physical lines
 * and only special-cased C-style block comments ({@code /* ... *}{@code /}).
 * PR-2 widens that to a small set of delimiter pairs &mdash; {@code /*...*}{@code /},
 * {@code """..."""}, {@code '''...'''} &mdash; which unlocks Python / Kotlin /
 * Scala triple-quoted string literals without touching any grammar file.
 *
 * <p>Each test builds a tiny grammar around the target token and feeds it a
 * payload that exercises the relevant physical-line shape.
 */
public class MultiLineTokenTest {

    /** Sanity check: a triple-quoted string fully on one line must still parse. */
    @Test
    void triple_quote_within_single_line() throws Exception {
        MyParser p = tripleQuoteParser("\"\"\"hello\"\"\"\n");
        assertNotNull(p.parse(), "single-line triple-quoted string should parse");
    }

    /** Opener and closer on different physical lines: two-line span. */
    @Test
    void triple_quote_spans_two_lines() throws Exception {
        MyParser p = tripleQuoteParser("\"\"\"hello\nworld\"\"\"\n");
        assertNotNull(p.parse(), "triple-quoted string spanning two lines should parse");
    }

    /** Opener, blank middle line, closer on third line. */
    @Test
    void triple_quote_spans_three_lines_with_empty_middle() throws Exception {
        MyParser p = tripleQuoteParser("\"\"\"line1\n\nline3\"\"\"\n");
        assertNotNull(p.parse(), "triple-quoted string with empty interior line should parse");
    }

    /**
     * Regression guard: pre-existing C-style block comment multi-line behaviour
     * must keep working under the generalised scanner.
     */
    @Test
    void block_comment_spans_lines() throws Exception {
        String lexer = """
                lexer grammar L;
                BLOCK_COMMENT : '/*' .*? '*/' ;
                WORD          : [A-Za-z]+ ;
                """;
        String parser = """
                parser grammar P;
                options {
                    tokenVocab = L;
                }
                root : BLOCK_COMMENT WORD ;
                """;
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(parser), new StringReader(lexer));
        MyParser p = b.createParser(new StringReader("/* a\n  b\n  c */\nend\n"));
        assertNotNull(p.parse(), "legacy /*...*/ multi-line behaviour must be preserved");
    }

    private static MyParser tripleQuoteParser(String input) throws Exception {
        String q3 = "'\"\"\"'";
        String lexer =
                "lexer grammar L;\n" +
                "TSTR : " + q3 + " .*? " + q3 + " ;\n";
        String parser =
                "parser grammar P;\n" +
                "options {\n    tokenVocab = L;\n}\n" +
                "root : TSTR ;\n";
        MyParserBuilder b = new MyParserBuilder(
                new StringReader(parser), new StringReader(lexer));
        return b.createParser(new StringReader(input));
    }
}
