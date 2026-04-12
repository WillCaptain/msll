package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step13_TestJSONFinal {
    public static void main(String[] args) {
        System.out.println("=== Step 13: Test JSON Grammar (Final) ===\n");

        try {
            System.out.println("Loading JSON grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "jsonParser.gm",
                "jsonLexer.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            // Test 1: Empty object
            System.out.println("Test 1: Empty object '{}'");
            try {
                MyParser parser1 = builder.createParser("{}");
                var tree1 = parser1.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree1.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(200, e.getMessage().length())) + "\n");
            }

            // Test 2: Simple object
            System.out.println("Test 2: Simple object");
            try {
                MyParser parser2 = builder.createParser("{\"name\": \"John\"}");
                var tree2 = parser2.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree2.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(200, e.getMessage().length())) + "\n");
            }

            // Test 3: Empty array
            System.out.println("Test 3: Empty array '[]'");
            try {
                MyParser parser3 = builder.createParser("[]");
                var tree3 = parser3.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree3.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(200, e.getMessage().length())) + "\n");
            }

            // Test 4: Simple array
            System.out.println("Test 4: Simple array");
            try {
                MyParser parser4 = builder.createParser("[1, 2, 3]");
                var tree4 = parser4.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree4.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(200, e.getMessage().length())) + "\n");
            }

            System.out.println("=== All tests completed ===");

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
