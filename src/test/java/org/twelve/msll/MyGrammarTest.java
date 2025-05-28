package org.twelve.msll;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.parser.MsllParser;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;
import org.twelve.msll.parsetree.*;
import org.twelve.msll.util.Constants;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class MyGrammarTest {
    private Grammars grammars;
    private final static String TEST_PARSER_GRAMMAR = "simpleExpressionParser.gm";
    private final static String TEST_LEXER_GRAMMAR = "simpleExpressionLexer.gm";
    private MyParserBuilder builder;
    private ParserGrammarTree parserGrammarTree;
    private LexerRuleTree lexerRuleTree;

    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder(TEST_PARSER_GRAMMAR,TEST_LEXER_GRAMMAR);
        this.parserGrammarTree = builder.parserGrammarTree();
        this.lexerRuleTree = builder.lexerGrammarTree();
        this.grammars = builder.grammars();
    }

    @Test
    void test_grammar_name() {
        assertEquals("SimpleExpressionParser",parserGrammarTree.name());
    }

    @Test
    void test_comments_from_grammar(){
        assertEquals(Constants.LONG_COMMENT,this.parserGrammarTree.start().node(0).symbol().name());
    }


    @Test
    void test_channel(){
        List<String> channels = lexerRuleTree.channels();
        assertEquals("HIDDEN",channels.get(0));
        assertEquals("ERROR",channels.get(1));
    }

    @Test
    void test_grammar_options() {
        Map<String, String> options = parserGrammarTree.options();
        assertEquals("SimpleExpressionLexer",options.get("tokenVocab"));
    }

    @Test
    void test_without_line_terminator(){
        MyParser parser = builder.createParser("let var_a=3+6; \n");
        ParserTree tree = parser.parse();
        assertEquals("var_a",((NonTerminalNode)tree.start().node(0)).node(1).toString());
    }

    @Test
    void test_comment_with_command(){
        MsllParser parser = builder.createParser("/*comments*/let a=4;");
        ParserTree tree = parser.parse();
        assertEquals("a",((NonTerminalNode)tree.start().node(1)).node(1).toString());
    }

    @Test
    void test_with_line_terminator(){
        try {
        builder.createParser("let"+System.lineSeparator()+" a=5;").parse();
            fail();
        }catch(Exception e){

        }
    }
    @Test
    void test_deep_nest(){
        //todo
    }

    @Test
    void test_multi_layers(){

    }
    @Test
    void test_flat(){

    }
}
