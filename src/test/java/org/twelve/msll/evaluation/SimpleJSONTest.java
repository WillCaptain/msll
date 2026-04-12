package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parsetree.ParserTree;

public class SimpleJSONTest {
    public static void main(String[] args) {
        try {
            System.out.println("Loading JSON grammar...");
            MyParserBuilder builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
            System.out.println("Grammar loaded successfully!");

            System.out.println("\nTesting simple object...");
            String json = "{\"name\": \"John\"}";
            MyParser parser = builder.createParser(json);
            ParserTree tree = parser.parse();
            System.out.println("Parse successful!");
            System.out.println("Tree: " + tree.start());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
