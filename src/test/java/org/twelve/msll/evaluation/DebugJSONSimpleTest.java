package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;

public class DebugJSONSimpleTest {
    public static void main(String[] args) {
        try {
            System.out.println("Step 1: Loading simple JSON grammar...");
            MyParserBuilder builder = new MyParserBuilder("jsonParser-simple.gm", "jsonLexer.gm");
            System.out.println("Step 2: Grammar loaded!");

            System.out.println("Step 3: Testing empty object...");
            var parser1 = builder.createParser("{}");
            var tree1 = parser1.parse();
            System.out.println("Step 4: Empty object parsed! " + tree1.start());

            System.out.println("Step 5: Testing simple pair...");
            var parser2 = builder.createParser("{\"name\": \"John\"}");
            var tree2 = parser2.parse();
            System.out.println("Step 6: Simple pair parsed! " + tree2.start());

            System.out.println("SUCCESS!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
