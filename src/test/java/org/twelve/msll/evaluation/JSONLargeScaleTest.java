package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.ParserTree;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Large-scale performance tests for JSON grammar.
 * Tests with 1K, 5K, and 10K JSON objects to demonstrate scalability.
 */
public class JSONLargeScaleTest {
    private MyParserBuilder builder;

    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
    }

    @Test
    void test_performance_1k_objects() {
        System.out.println("\n=== JSON 1K Objects Test ===");

        // Generate a JSON array with 1,000 objects
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i)
              .append(",\"name\":\"user").append(i).append("\"")
              .append(",\"active\":").append(i % 2 == 0 ? "true" : "false")
              .append(",\"score\":").append(i * 10)
              .append("}");
        }
        sb.append("]");

        String json = sb.toString();

        // Estimate tokens: each object has ~15 tokens
        int estimatedTokens = 1000 * 15 + 1000; // objects + commas/brackets

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Objects: 1000");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        assertTrue(tokensPerSecond > 100, "Throughput should be > 100 tokens/sec");
    }

    @Test
    void test_performance_5k_objects() {
        System.out.println("\n=== JSON 5K Objects Test ===");

        // Generate a JSON array with 5,000 objects
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 5000; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i)
              .append(",\"name\":\"user").append(i).append("\"")
              .append(",\"active\":").append(i % 2 == 0 ? "true" : "false")
              .append(",\"score\":").append(i * 10)
              .append("}");
        }
        sb.append("]");

        String json = sb.toString();

        // Estimate tokens: each object has ~15 tokens
        int estimatedTokens = 5000 * 15 + 5000;

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Objects: 5000");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        assertTrue(tokensPerSecond > 100, "Throughput should be > 100 tokens/sec");
    }

    @Test
    void test_performance_10k_objects() {
        System.out.println("\n=== JSON 10K Objects Test ===");

        // Generate a JSON array with 10,000 objects
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10000; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i)
              .append(",\"name\":\"user").append(i).append("\"")
              .append(",\"active\":").append(i % 2 == 0 ? "true" : "false")
              .append(",\"score\":").append(i * 10)
              .append("}");
        }
        sb.append("]");

        String json = sb.toString();

        // Estimate tokens: each object has ~15 tokens
        int estimatedTokens = 10000 * 15 + 10000;

        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        double tokensPerSecond = (estimatedTokens * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Objects: 10000");
        System.out.println("  Estimated tokens: " + estimatedTokens);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");

        assertTrue(tokensPerSecond > 100, "Throughput should be > 100 tokens/sec");
    }
}
