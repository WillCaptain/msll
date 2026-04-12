package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step5_TestV2Lexer {
    public static void main(String[] args) {
        System.out.println("=== Step 5: Test V2 Lexer ===\n");

        try {
            System.out.println("Loading JSON grammar with v2 lexer...");
            MyParserBuilder builder = new MyParserBuilder(
                "jsonParser-minimal.gm",
                "jsonLexer-v2.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            System.out.println("Creating parser for '{}'...");
            MyParser parser = builder.createParser("{}");
            System.out.println("✓ Parser created\n");

            System.out.println("Starting parse...");
            var tree = parser.parse();

            System.out.println("✓ Parse completed!");
            System.out.println("Tree: " + tree.start());

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
