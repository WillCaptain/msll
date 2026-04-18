package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression guard for the grammar-state leak between {@link MyParserBuilder}
 * instances.
 *
 * <p>Before the fix to {@link org.twelve.msll.grammarsymbol.Terminals#newMy}
 * and {@link org.twelve.msll.grammarsymbol.NonTerminals#newMy}, building a
 * {@code MyParserBuilder} for grammar A would mutate a static {@code my}
 * singleton; when a later {@code MyParserBuilder} was built for grammar B,
 * the build process would find A's lexer rules already registered and
 * emit a predict table that quoted A's patterns on B's parse failures.
 *
 * <p>This test picks two grammars with <em>completely disjoint</em> token sets:
 * <ul>
 *   <li>Grammar A has only {@code DIGIT}s.</li>
 *   <li>Grammar B has only {@code LETTER}s.</li>
 * </ul>
 * If the singletons are shared, B's parser would recognise input like
 * {@code "1a2"} (DIGIT from A leaking in) instead of rejecting it. With the
 * fix in place, B strictly accepts letter-only input and A strictly accepts
 * digit-only input, regardless of construction order.
 */
public class NoGrammarLeakTest {

    private static final String LEXER_A = """
            lexer grammar A;
            DIGIT : [0-9]+ ;
            """;
    private static final String PARSER_A = """
            parser grammar APar;
            options {
                tokenVocab = A;
            }
            root : DIGIT ;
            """;

    private static final String LEXER_B = """
            lexer grammar B;
            LETTER : [a-zA-Z]+ ;
            """;
    private static final String PARSER_B = """
            parser grammar BPar;
            options {
                tokenVocab = B;
            }
            root : LETTER ;
            """;

    @Test
    void builders_do_not_leak_terminals_across_instances() throws Exception {
        // Build A first (registers DIGIT in whatever collection A uses).
        MyParserBuilder bA = new MyParserBuilder(
                new StringReader(PARSER_A), new StringReader(LEXER_A));
        // Build B second. If DIGIT leaked in, B's parser would accept "123".
        MyParserBuilder bB = new MyParserBuilder(
                new StringReader(PARSER_B), new StringReader(LEXER_B));

        // Sanity: each grammar accepts its own input.
        MyParser pA = bA.createParser(new StringReader("42"));
        assertNotNull(pA.parse(), "A should accept digits");

        MyParser pB = bB.createParser(new StringReader("hello"));
        assertNotNull(pB.parse(), "B should accept letters");

        // The definitive anti-leak check: B rejects digits. If the fix
        // regresses, B will try to parse "42" as DIGIT and succeed.
        boolean rejected = false;
        try {
            MyParser polluted = bB.createParser(new StringReader("42"));
            Object tree = polluted.parse();
            if (tree == null) rejected = true;
        } catch (Throwable t) {
            rejected = true;
        }
        if (!rejected) {
            throw new AssertionError(
                    "Grammar B accepted digit-only input '42'; token state from "
                            + "grammar A leaked into B's predict table.");
        }
    }
}
