package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step12_TestMultipleTokens {
    public static void main(String[] args) {
        System.out.println("=== Step 12: Test Multiple Tokens ===\n");

        try {
            System.out.println("Loading JavaScript grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "javascriptParser-simple.gm",
                "javascriptLexer-simple.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            // Test 1: Single token (should fail or hang)
            System.out.println("Test 1: Single semicolon ';'");
            try {
                MyParser parser1 = builder.createParser(";");
                var tree1 = parser1.parse();
                System.out.println("✓ Parsed\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())) + "\n");
            }

            // Test 2: Two tokens (should fail or hang)
            System.out.println("Test 2: Two tokens '{}'");
            try {
                MyParser parser2 = builder.createParser("{}");
                var tree2 = parser2.parse();
                System.out.println("✓ Parsed\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())) + "\n");
            }

            // Test 3: Multiple tokens (should work)
            System.out.println("Test 3: Multiple tokens 'let x = 5;'");
            try {
                MyParser parser3 = builder.createParser("let x = 5;");
                var tree3 = parser3.parse();
                System.out.println("✓ Parsed successfully!\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())) + "\n");
            }

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
