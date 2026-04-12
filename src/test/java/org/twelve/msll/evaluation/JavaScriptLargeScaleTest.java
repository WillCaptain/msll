package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.ParserTree;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Large-scale performance tests for JavaScript grammar.
 * Tests with 10K, 50K, and 100K tokens to demonstrate scalability.
 */
public class JavaScriptLargeScaleTest {
    private MyParserBuilder builder;

    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder("javascriptParser-simple.gm", "javascriptLexer-simple.gm");
    }

    @Test
    void test_performance_10k_tokens() {
        System.out.println("\n=== JavaScript 10K Tokens Test ===");

        // Generate diverse JavaScript code with ~10,000 tokens
        StringBuilder sb = new StringBuilder();

        // 1. Variable declarations (500 statements, ~2500 tokens)
        for (int i = 0; i < 500; i++) {
            sb.append("let var").append(i).append(" = ").append(i).append(";\n");
        }

        // 2. Function declarations (100 functions, ~3000 tokens)
        for (int i = 0; i < 100; i++) {
            sb.append("function func").append(i).append("(a, b) { return a + b; }\n");
        }

        // 3. If statements (100 statements, ~2500 tokens)
        for (int i = 0; i < 100; i++) {
            sb.append("if (x").append(i).append(" > 0) { return x").append(i).append("; }\n");
        }

        // 4. Expressions (200 statements, ~2000 tokens)
        for (int i = 0; i < 200; i++) {
            sb.append("let result").append(i).append(" = a + b * c - d;\n");
        }

        String js = sb.toString();

        // Count actual tokens (rough estimate)
        int estimatedTokens = 500 * 5 + 100 * 11 + 100 * 10 + 200 * 11;

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Statements: 900");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        // Assert reasonable performance (at least 1000 tokens/sec)
        assertTrue(tokensPerSecond > 1000, "Throughput should be > 1000 tokens/sec");
    }

    @Test
    void test_performance_50k_tokens() {
        System.out.println("\n=== JavaScript 50K Tokens Test ===");

        // Generate diverse JavaScript code with ~50,000 tokens
        StringBuilder sb = new StringBuilder();

        // 1. Variable declarations (2500 statements, ~12500 tokens)
        for (int i = 0; i < 2500; i++) {
            sb.append("let var").append(i).append(" = ").append(i).append(";\n");
        }

        // 2. Function declarations (500 functions, ~15000 tokens)
        for (int i = 0; i < 500; i++) {
            sb.append("function func").append(i).append("(a, b) { return a + b; }\n");
        }

        // 3. If statements (500 statements, ~12500 tokens)
        for (int i = 0; i < 500; i++) {
            sb.append("if (x").append(i).append(" > 0) { return x").append(i).append("; }\n");
        }

        // 4. Expressions (1000 statements, ~10000 tokens)
        for (int i = 0; i < 1000; i++) {
            sb.append("let result").append(i).append(" = a + b * c - d;\n");
        }

        String js = sb.toString();

        // Count actual tokens (rough estimate)
        int estimatedTokens = 2500 * 5 + 500 * 11 + 500 * 10 + 1000 * 11;

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Statements: 4500");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        // Assert reasonable performance
        assertTrue(tokensPerSecond > 1000, "Throughput should be > 1000 tokens/sec");
    }

    @Test
    void test_performance_100k_tokens() {
        System.out.println("\n=== JavaScript 100K Tokens Test ===");

        // Generate diverse JavaScript code with ~100,000 tokens
        StringBuilder sb = new StringBuilder();

        // 1. Variable declarations (5000 statements, ~25000 tokens)
        for (int i = 0; i < 5000; i++) {
            sb.append("let var").append(i).append(" = ").append(i).append(";\n");
        }

        // 2. Function declarations (1000 functions, ~30000 tokens)
        for (int i = 0; i < 1000; i++) {
            sb.append("function func").append(i).append("(a, b) { return a + b; }\n");
        }

        // 3. If statements (1000 statements, ~25000 tokens)
        for (int i = 0; i < 1000; i++) {
            sb.append("if (x").append(i).append(" > 0) { return x").append(i).append("; }\n");
        }

        // 4. Expressions (2000 statements, ~20000 tokens)
        for (int i = 0; i < 2000; i++) {
            sb.append("let result").append(i).append(" = a + b * c - d;\n");
        }

        String js = sb.toString();

        // Count actual tokens (rough estimate)
        int estimatedTokens = 5000 * 5 + 1000 * 11 + 1000 * 10 + 2000 * 11;

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(js);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Statements: 9000");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        // Assert reasonable performance
        assertTrue(tokensPerSecond > 1000, "Throughput should be > 1000 tokens/sec");
    }
}
