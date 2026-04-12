package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;

public class DebugJSONTest {
    public static void main(String[] args) {
        try {
            System.out.println("Step 1: Loading JSON grammar...");
            MyParserBuilder builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
            System.out.println("Step 2: Grammar loaded successfully!");

            System.out.println("Step 3: Creating parser...");
            String json = "{\"name\": \"John\"}";
            System.out.println("Step 4: Input: " + json);

            System.out.println("Step 5: About to create parser...");
            var parser = builder.createParser(json);
            System.out.println("Step 6: Parser created!");

            System.out.println("Step 7: About to parse...");
            var tree = parser.parse();
            System.out.println("Step 8: Parse successful!");
            System.out.println("Step 9: Tree: " + tree.start());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
