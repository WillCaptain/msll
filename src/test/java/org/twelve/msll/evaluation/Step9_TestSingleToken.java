package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step9_TestSingleToken {
    public static void main(String[] args) {
        System.out.println("=== Step 9: Test Single Token ===\n");

        try {
            System.out.println("Loading single-token grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "testSingleToken.gm",
                "javascriptLexer-simple.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            System.out.println("Testing: '{'");
            MyParser parser = builder.createParser("{");
            System.out.println("✓ Parser created");

            System.out.println("Starting parse...");
            var tree = parser.parse();

            System.out.println("✓✓✓ SUCCESS! ✓✓✓");
            System.out.println("Tree: " + tree.start());

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
