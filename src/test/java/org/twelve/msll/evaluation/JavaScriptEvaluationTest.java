package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.ParserTree;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Evaluation tests for JavaScript grammar.
 * Tests left recursion handling and complex expressions.
 */
public class JavaScriptEvaluationTest {
    private MyParserBuilder builder;

    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder("javascriptParser-simple.gm", "javascriptLexer-simple.gm");
    }

    @Test
    void test_simple_variable() {
        String js = "let x = 5;";
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
        System.out.println("✓ Simple variable declaration parsed");
    }

    @Test
    void test_function_declaration() {
        String js = "function add(a, b) { return a + b; }";
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
        System.out.println("✓ Function declaration parsed");
    }

    @Test
    void test_left_recursive_expression() {
        String js = "let result = a + b * c - d;";
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
        System.out.println("✓ Left-recursive expression parsed");
    }

    @Test
    void test_if_statement() {
        String js = "if (x > 0) { return x; }";
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
        System.out.println("✓ If statement parsed");
    }

    @Test
    void test_performance_medium_js() {
        // Generate JavaScript code with 1000 statements
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("let var").append(i).append(" = ").append(i).append(";\n");
        }

        String js = sb.toString();
        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        // Rough token count: each statement has ~5 tokens
        int tokenCount = 1000 * 5;
        double tokensPerSecond = (tokenCount * 1000.0) / duration;

        System.out.println("JavaScript Performance:");
        System.out.println("  Statements: 1000");
        System.out.println("  Tokens: ~" + tokenCount);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");
    }
}
