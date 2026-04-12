package org.twelve.msll.evaluation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to check if JSON grammar loads
 */
public class JSONGrammarLoadTest {

    @Test
    @SneakyThrows
    void test_load_json_grammar() {
        MyParserBuilder builder = new MyParserBuilder("jsonParser.gm", "jsonLexer.gm");
        assertNotNull(builder);
        System.out.println("JSON grammar loaded successfully!");
    }
}
