package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test if JavaScript grammar loads
 */
public class JavaScriptGrammarLoadTest {

    @Test
    @SneakyThrows
    void test_load_javascript_grammar() {
        MyParserBuilder builder = new MyParserBuilder("javascriptParser.gm", "javascriptLexer.gm");
        assertNotNull(builder);
        System.out.println("JavaScript grammar loaded successfully!");
    }
}
