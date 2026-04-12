package org.twelve.msll.evaluation;

import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parser.MyParser;

public class DebugPythonTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Debug Python Grammar ===\n");

            System.out.println("Loading Python grammar...");
            MyParserBuilder builder = new MyParserBuilder(
                "pythonParser-simple.gm",
                "pythonLexer-simple.gm"
            );
            System.out.println("✓ Grammar loaded\n");

            // Test 1: Simplest possible input
            System.out.println("Test 1: 'x = 5\\n'");
            try {
                MyParser parser1 = builder.createParser("x = 5\n");
                var tree1 = parser1.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree1.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(300, e.getMessage().length())) + "\n");
            }

            // Test 2: Without newline
            System.out.println("Test 2: 'x = 5' (no newline)");
            try {
                MyParser parser2 = builder.createParser("x = 5");
                var tree2 = parser2.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree2.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(300, e.getMessage().length())) + "\n");
            }

            // Test 3: Two statements
            System.out.println("Test 3: Two statements");
            try {
                MyParser parser3 = builder.createParser("x = 5\ny = 10\n");
                var tree3 = parser3.parse();
                System.out.println("✓ Parsed successfully!");
                System.out.println("Tree: " + tree3.start() + "\n");
            } catch (Exception e) {
                System.out.println("✗ Failed: " + e.getMessage().substring(0, Math.min(300, e.getMessage().length())) + "\n");
            }

        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
