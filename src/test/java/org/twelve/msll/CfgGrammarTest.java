package org.twelve.msll;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.grammar.Grammar;
import org.twelve.msll.grammar.Production;
import org.twelve.msll.parser.GrammarPredicate;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.NonTerminalNode;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.twelve.msll.util.Tool.cast;

/**
 * Unit tests for context-free grammar transformations and predicate parsing.
 * huizi 
 */
public class CfgGrammarTest {
    private final String BETA = "a_beta'";
    private final String ALPHA = "a_alpha'";
    private final String ALPHA_1 = "a_alpha_1'";
    private final String ALPHA_2 = "a_alpha_2'";

    /**
     * Tests whether long comments in the grammar are parsed correctly and recognized as special tokens.
     */
    @Test
    @SneakyThrows
    void test_long_comments() {
        MyParserBuilder builder = new MyParserBuilder(new StringReader("e:ID PLUS e;\n /*comments\n comments\n*/\n f:ID PLUS f;"), new StringReader("ID:\"abc\";"));
        assertEquals(2, builder.grammars().grammars().size());
        NonTerminalNode grammars = cast(builder.parserGrammarTree().start().node(0));
        assertEquals("LONG_COMMENT", grammars.node(1).name());

    }

    /**
     * Tests left-recursion elimination in grammar rules.
     * Original: A → Aα1 | Aα2 | β1 | β2
     * Transformed:
     *   A → β A'
     *   A' → α1 A' | α2 A' | ε
     */
    @Test
    @SneakyThrows
    void test_transform_left_recursion() {
        // A → Aα1 |Aα2 |...|Aαn | β1 | β2 |...|βn
        //--------eliminate left recursion----------------
        // β → β1 | β2 |...|βn
        // A  → β A0'
        // A0' → A1'|...|Ai' | ε
        //Ai' → αi A0'
        MyParserBuilder builder = new MyParserBuilder(new StringReader("a:a ALPHA | a DELTA | BETA | GAMMA;"),
                new StringReader("ALPHA:\"alpha\"; BETA:\"beta\"; GAMMA:\"gamma\"; DELTA:\"DELTA\";"));
        List<Grammar> grammars = builder.grammars().grammars();
        assertEquals(5, grammars.size());
        Grammar main = grammars.get(0);
        assertEquals(1, main.productions().size());
        assertEquals("a", main.name());
        //A  → β A0'
        Production top = main.productions().get(0);
        assertEquals(BETA, top.symbols().get(0).name());
        assertEquals(ALPHA, top.symbols().get(1).name());
        //A1'  → α1 A0'
        Production alpha = grammars.get(2).productions().get(0);
        assertEquals("ALPHA", alpha.symbols().get(0).name());
        assertEquals(ALPHA, alpha.symbols().get(1).name());
        //A2'  → α2 A0'
        Production delta = grammars.get(3).productions().get(0);
        assertEquals("DELTA", delta.symbols().get(0).name());
        assertEquals(ALPHA, delta.symbols().get(1).name());

        // β → β1 | β2 |...|βn
        Grammar beta = grammars.get(1);
        assertEquals(BETA, beta.name());
        assertEquals(2, beta.productions().size());
        //β1
        assertEquals("BETA", beta.productions().get(0).symbols().get(0).name());
        //β2
        assertEquals("GAMMA", beta.productions().get(1).symbols().get(0).name());

        // A0' → A1‘ |...| Ai' | ε
        Grammar a0 = grammars.get(4);
        assertEquals(ALPHA, a0.name());
        assertEquals("EPSILON", a0.productions().get(0).symbols().get(0).name());
        assertEquals(ALPHA_1, a0.productions().get(1).symbols().get(0).name());
        assertEquals(ALPHA_2, a0.productions().get(2).symbols().get(0).name());

    }

    /**
     * Tests parsing of associativity annotations.
     */
    @Test
    @SneakyThrows
    void test_assoc_addition() {
        MyParserBuilder builder = new MyParserBuilder(new StringReader("a:<assoc = right>ID PLUS a;"),
                new StringReader("ID:/\"[abc]+\"/; PLUS:\"+\";"));
        assertEquals("RIGHT", builder.grammars().grammars().get(0).productions().get(0).assoc().name());
    }

    /**
     * Tests parsing of inline code-based predicates inside a production rule.
     */
    @Test
    @SneakyThrows
    void test_code_addition() {
        MyParserBuilder builder = new MyParserBuilder(new StringReader("a: {notLineTerminator()} PLUS a;"),
                new StringReader("ID:/\"[abc]+\"/; PLUS:\"+\";"));
        assertTrue(builder.grammars().grammars().get(0).productions().get(0).symbols().get(0).type().name().contains("PREDICATE"));//TODO
        assertEquals("{notLineTerminator()}", builder.grammars().grammars().get(0).productions().get(0).symbols().get(0).name());//TODO
    }

    /**
     * Tests parsing of a GrammarPredicate from a raw function-like string.
     */
    @Test
    void test_predicate_grammar_parse() {
        GrammarPredicate predicate = new GrammarPredicate("this.abc(1,\"abc\")");
        assertEquals("abc", predicate.funcName());
        assertEquals(1L, predicate.args().get(0));
        assertEquals("abc", predicate.args().get(1));
    }
}
