package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step7_TestJSWithBraces {
    public static void main(String[] args) {
        System.out.println("=== Step 7: Test JavaScript Parser with {} ===\n");

        try {
            System.out.println("Loading JavaScript grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "javascriptParser-simple.gm",
                "javascriptLexer-simple.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            // JavaScript's block is: LBRACE statement* RBRACE
            // So {} should be a valid empty block
            System.out.println("Test 1: Empty block as statement");
            try {
                MyParser parser1 = builder.createParser("{}");
                var tree1 = parser1.parse();
                System.out.println("✓ Empty block parsed!");
                System.out.println("Tree: " + tree1.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage() + "\n");
            }

            // Try with a statement inside
            System.out.println("Test 2: Block with return statement");
            try {
                MyParser parser2 = builder.createParser("{ return 5; }");
                var tree2 = parser2.parse();
                System.out.println("✓ Block with statement parsed!");
                System.out.println("Tree: " + tree2.start());
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
