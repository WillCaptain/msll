package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;

public class Step3_CompareGrammars {
    public static void main(String[] args) {
        System.out.println("=== Step 3: Compare Grammar Loading ===\n");

        // Test 1: JavaScript grammar (we know this works)
        System.out.println("Test 1: Loading JavaScript grammar...");
        try {
            MyParserBuilder jsBuilder = new MyParserBuilder(
                "javascriptParser-simple.gm",
                "javascriptLexer-simple.gm"
            );
            System.out.println("✓ JavaScript grammar loaded successfully\n");
        } catch (Exception e) {
            System.out.println("✗ JavaScript grammar failed: " + e.getMessage() + "\n");
        }

        // Test 2: JSON minimal grammar
        System.out.println("Test 2: Loading JSON minimal grammar...");
        try {
            MyParserBuilder jsonBuilder = new MyParserBuilder(
                "jsonParser-minimal.gm",
                "jsonLexer.gm"
            );
            System.out.println("✓ JSON grammar loaded successfully\n");
        } catch (Exception e) {
            System.out.println("✗ JSON grammar failed: " + e.getMessage() + "\n");
            e.printStackTrace();
        }

        System.out.println("=== Test completed ===");
    }
}
