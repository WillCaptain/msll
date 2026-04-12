package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class Step2_MinimalJSONTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Step 2: Minimal JSON Grammar ===\n");
            System.out.println("Grammar: root : LBRACE RBRACE ;\n");

            System.out.println("Loading grammar...");
            MyParserBuilder builder = new MyParserBuilder("jsonParser-minimal.gm", "jsonLexer.gm");
            System.out.println("✓ Grammar loaded\n");

            System.out.println("Testing: '{}'");
            MyParser parser = builder.createParser("{}");
            System.out.println("✓ Parser created");

            var tree = parser.parse();
            System.out.println("✓ Parsed successfully!");
            System.out.println("Tree: " + tree.start());

            System.out.println("\n=== SUCCESS ===");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
