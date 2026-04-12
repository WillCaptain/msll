package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.ParserTree;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Evaluation tests for JSON grammar.
 * Tests correctness and measures performance on JSON parsing.
 */
public class JSONEvaluationTest {
    private MyParserBuilder builder;

    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
    }

    @Test
    void test_simple_object() {
        MyParser parser = builder.createParser("{\"name\": \"John\", \"age\": 30}");
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
    }

    @Test
    void test_nested_object() {
        String json = """
            {
                "person": {
                    "name": "Alice",
                    "address": {
                        "city": "NYC",
                        "zip": "10001"
                    }
                }
            }
            """;
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
    }

    @Test
    void test_array() {
        String json = "[1, 2, 3, 4, 5]";
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
    }

    @Test
    void test_mixed_array() {
        String json = "[\"string\", 123, true, false, null, {\"key\": \"value\"}]";
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        assertNotNull(tree);
        assertNotNull(tree.start());
    }

    @Test
    void test_performance_large_json() {
        // Generate a JSON array with 100 objects
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(i)
              .append(",\"name\":\"user").append(i).append("\"")
              .append(",\"active\":").append(i % 2 == 0 ? "true" : "false")
              .append("}");
        }
        sb.append("]");

        String json = sb.toString();
        long startTime = System.currentTimeMillis();
        MyParser parser = builder.createParser(json);
        ParserTree tree = parser.parse();
        long endTime = System.currentTimeMillis();

        assertNotNull(tree);
        assertNotNull(tree.start());

        long duration = endTime - startTime;
        int tokenCount = json.split("\\s+|(?=[{}\\[\\]:,])|(?<=[{}\\[\\]:,])").length;
        double tokensPerSecond = (tokenCount * 1000.0) / duration;

        System.out.println("JSON Performance:");
        System.out.println("  Tokens: " + tokenCount);
        System.out.println("  Time: " + duration + " ms");
        System.out.println("  Throughput: " + String.format("%.0f", tokensPerSecond) + " tokens/sec");
    }
}

