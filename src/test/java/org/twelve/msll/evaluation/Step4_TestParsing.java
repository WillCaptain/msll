package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step4_TestParsing {
    public static void main(String[] args) {
        System.out.println("=== Step 4: Test Parsing ===\n");

        try {
            System.out.println("Loading JSON minimal grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "jsonParser-minimal.gm",
                "jsonLexer.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            System.out.println("Creating parser for '{}'...");
            MyParser parser = builder.createParser("{}");
            System.out.println("✓ Parser created\n");

            System.out.println("Starting parse...");
            System.out.println("(This is where it might hang)\n");

            var tree = parser.parse();

            System.out.println("✓ Parse completed!");
            System.out.println("Tree: " + tree.start());

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
