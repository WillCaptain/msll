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

import static org.junit.jupiter.api.Assertions.*;
import static org.twelve.msll.util.Tool.cast;

/**
 * Unit tests for parser and lexer grammar functionality using the MSLL infrastructure.
 * Tests include grammar name recognition, comment parsing, channel handling, grammar options,
 * and line terminator behavior.
 * huizi
 */
public class MyGrammarTest {
    private Grammars grammars;
    private final static String TEST_PARSER_GRAMMAR = "outlineParser.gm";
    private final static String TEST_LEXER_GRAMMAR = "outlineLexer.gm";
    private MyParserBuilder builder;
    private ParserGrammarTree parserGrammarTree;
    private LexerRuleTree lexerRuleTree;

    /**
     * Setup method to initialize the parser builder and extract grammar trees before each test.
     */
    @BeforeEach
    @SneakyThrows
    void setup() {
        this.builder = new MyParserBuilder(TEST_PARSER_GRAMMAR, TEST_LEXER_GRAMMAR);
        this.parserGrammarTree = builder.parserGrammarTree();
        this.lexerRuleTree = builder.lexerGrammarTree();
        this.grammars = builder.grammars();
    }

    /**
     * Verifies the name of the parser grammar is correctly parsed.
     */
    @Test
    void test_grammar_name() {
        assertEquals("OutlineParser", parserGrammarTree.name());
    }

    /**
     * Ensures long comments are correctly extracted from the start node in the grammar.
     */
    @Test
    void test_comments_from_grammar() {
        assertEquals(Constants.LONG_COMMENT, this.parserGrammarTree.start().node(0).symbol().name());
    }

    /**
     * Confirms the lexer rule tree correctly identifies and stores named channels.
     */
    @Test
    void test_channel() {
        List<String> channels = lexerRuleTree.channels();
        assertEquals("HIDDEN", channels.get(0));
        assertEquals("ERROR", channels.get(1));
    }

    /**
     * Validates grammar-level options like token vocabulary references.
     */
    @Test
    void test_grammar_options() {
        Map<String, String> options = parserGrammarTree.options();
        assertEquals("OutlineLexer", options.get("tokenVocab"));
    }

    /**
     * Tests that valid input without line terminators is parsed correctly.
     */
    @Test
    void test_without_line_terminator() {
        MyParser parser = builder.createParser("let var_a=3+6;\n let var_b = 7;");
        ParserTree tree = parser.parse();
        assertEquals("var_a", ((NonTerminalNode) ((NonTerminalNode) tree.start().node(0)).node(1)).node(0).toString());
        assertEquals("var_b", ((NonTerminalNode) ((NonTerminalNode) tree.start().node(1)).node(1)).node(0).toString());
    }

    /**
     * Verifies that comments preceding a command are handled properly by the parser.
     */
    @Test
    void test_comment_with_command() {
        MsllParser parser = builder.createParser("/*line1 \n line2*/ \n let a=4;");
        ParserTree tree = parser.parse();
        assertEquals("a", ((NonTerminalNode) ((NonTerminalNode) tree.start().node(1)).node(1)).node(0).toString());
        assertEquals("MultiLineComment", tree.start().node(0).symbol().name());
    }
    
    /**
     * Ensures that parsing fails when encountering a line terminator in a restricted context.
     */
    @Test
    void test_multi_layers() {
        /**
         * test execution time for 100 times is different from machines
         * in mac air m1 2020, the average execution time is around 40ms
         */
        String code = """
                    let me = {
                    let age = 40;
                    {{{{{
                        age: age,
                        name: { first: "Will", last: "Zhang" },
                        friends: ["Evan": {
                            age: 20,
                            name: { first: "Evan", last: "Zhang" }
                        }],
                        make_friend: friend -> this.friends.put(friend.name[0], friend)
                    }}}}}
                };""";
        MsllParser<?> parser = builder.createParser(code);
        for (int i = 0; i < 99; i++) {
            parser.parse();
        }
        ParserTree tree = parser.parse();
        assertEquals(5, parser.maxStackSize());//maximum online stacks
        assertEquals(45, parser.totalStackSize());//due to the code change, 45 is different from the number（39） in paper
        //verify the parsing result is correct
        //let me = ... is a variant declarator
        NonTerminalNode varDeclarator = cast(tree.start().node(0));
        assertEquals("variableDeclarator", varDeclarator.name());
        //let .. ={..} is a block
        NonTerminalNode block = cast(((NonTerminalNode) varDeclarator.node(1)).node(2));
        assertEquals("block", block.name());
        //{{...}} next block
        block = cast(block.node(2));
        //{{{...}}} next block
        block = cast(block.node(1));
        assertEquals("block", block.name());
        //{{{{...}}}} next block
        block = cast(block.node(1));
        assertEquals("block", block.name());
        //{{{{{...}}}}} next block
        block = cast(block.node(1));
        assertEquals("block", block.name());
        //{{{{{{age:....}}}}}} is entity
        NonTerminalNode entity = cast(block.node(1));
        assertEquals("entity", entity.name());
        //name:{..} is entity
        NonTerminalNode property = cast(entity.node(3));
        assertEquals("property_assignment", property.name());
        assertEquals("entity", property.node(2).name());
        //friend:[] is a map
        property = cast(entity.node(5));
        NonTerminalNode map = cast(property.node(2));
        assertEquals("map", map.name());
        //makeFriend is a lambda
        property = cast(entity.node(7));
        NonTerminalNode lambda = cast(property.node(2));
        assertEquals("lambda", lambda.name());
    }

    @Test
    void test_deep_layers() {

    }

    @Test
    void test_with_line_terminator() {
        try {
            builder.createParser("let" + System.lineSeparator() + " a=5;").parse();
            fail();
        } catch (Exception e) {

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
