package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step1_JSONLexerTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Step 1: Test JSON Lexer ===\n");

            MyParserBuilder builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
            System.out.println("✓ Grammar loaded\n");

            // Test 1: Single tokens
            System.out.println("Test 1: Single left brace");
            testLexer(builder, "{");

            System.out.println("\nTest 2: Single right brace");
            testLexer(builder, "}");

            System.out.println("\nTest 3: Empty object");
            testLexer(builder, "{}");

            System.out.println("\nTest 4: String literal");
            testLexer(builder, "\"hello\"");

            System.out.println("\nTest 5: Colon");
            testLexer(builder, ":");

            System.out.println("\n=== All lexer tests completed ===");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testLexer(MyParserBuilder builder, String input) {
        try {
            System.out.println("  Input: '" + input + "'");
            MyParser parser = builder.createParser(input);

            // Try to get tokens - we need to find the right API
            System.out.println("  Parser created successfully");

            // For now, just try to parse and see what happens
            var tree = parser.parse();
            System.out.println("  ✓ Parsed successfully");

        } catch (Exception e) {
            System.out.println("  ✗ Error: " + e.getMessage());
        }
    }
}
