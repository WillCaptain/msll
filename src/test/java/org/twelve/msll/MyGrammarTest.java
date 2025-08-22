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

import java.util.Date;
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

    @Test
    void test_with_line_terminator() {
        try {
            builder.createParser("let" + System.lineSeparator() + " a=5;").parse();
            fail();
        } catch (Exception e) {

        }
    }

    /**
     * msll paper: section 7 case 1
     */
    @Test
    void test_multi_layers() {
        /**
         * test execution time for 1000 times is different from machines
         * in mac air m1 2020, the average execution time is around 70-85ms
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
                };
                me.make_friend({
                    name: ("Noble", "Zhang"),
                    age: 10
                });
                
                {{{{
                    let more = 100;
                    me.friends.get("Noble").age + me.age + more
                }}}}""";
        MsllParser<?> parser = builder.createParser(code);
        for (int i = 0; i < 999; i++) {
            parser.parse();
        }
        ParserTree tree = parser.parse();
        assertEquals(5, parser.maxStackSize());//maximum online stacks
        assertEquals(74, parser.totalStackSize());//due to the grammar change, 74 is different from the number（39） in paper
        //verify the parsing result is correct
        //let me = ... is a variant declarator
        NonTerminalNode varDeclarator = cast(tree.start().node(0));
        assertEquals("variable_declarator", varDeclarator.name());
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

        //me.make_friend(....)
        NonTerminalNode emptyStatement = cast(tree.start().node(1));
        assertEquals("empty_statement",emptyStatement.name());

        //{{{{...}}}} return block
        NonTerminalNode returnBlock = cast(tree.start().node(2));
        assertEquals("block",returnBlock.name());

        StringBuilder longCode = new StringBuilder();
        //447k tokens
        for (int i=0; i<1000; i++){
            longCode.append(code);
        }
        long beginTime = new Date().getTime();
        Runtime rt = Runtime.getRuntime();
        long beginMemory = rt.totalMemory() - rt.freeMemory();
        parser = builder.createParser(longCode.toString());
        parser.parse();
        long duration = new Date().getTime()-beginTime;
        long memory = (rt.totalMemory() - rt.freeMemory() - beginMemory)/(1024*1024);
        assertEquals(5, parser.maxStackSize());//maximum online stacks
        assertEquals(73001, parser.totalStackSize());


    }

    /**
     * msll paper: section 7 case 2
     */
    @Test
    void test_deep_layers() {
        /**
         * test execution time for 1000 times is different from machines
         * in mac air m1 2020, the average execution time is around 80-84ms
         */
        String code = """
                me.make_friend({
                    name:("Noble","Zhang"),
                    age:1,
                    friends:[{
                        name:{
                            last:"a",
                            first:"b",
                            friends:[{
                                name:"c",
                                friends:[{
                                    name:"d",
                                    friends:["name":"e"]
                }]}]}}]});
                var result = fx(x,y,z){ { { {
                	let more = 1+counter;
                	me.friends.get("Noble").age+me.age+more+x(y,z)
                } } }};
                counter += result((a,b)->a+b,1,2);""";
        MsllParser<?> parser = builder.createParser(code);
        /*for (int i = 0; i < 999; i++) {
            parser.parse();
        }
        ParserTree tree = parser.parse();
        assertEquals(33, parser.maxStackSize());//maximum online stacks
        assertEquals(181, parser.totalStackSize());//due to the grammar change, 181 is different from the number（2989） in paper
        //verify the parse tree
        NonTerminalNode makeFriends = cast(((NonTerminalNode) tree.start().node(0)).node(0));
        assertEquals("factor_expression",makeFriends.name());
        NonTerminalNode result = cast(tree.start().node(1));
        assertEquals("variable_declarator",result.name());
        NonTerminalNode counter = cast(tree.start().node(2));
        assertEquals("assignment",counter.name());
*/
        StringBuilder longCode = new StringBuilder();
        //432k tokens
        for (int i=0; i<1000; i++){
            longCode.append(code);
        }
        long begin = new Date().getTime();
        Runtime rt = Runtime.getRuntime();
        long beginMemory = rt.totalMemory() - rt.freeMemory();
        parser = builder.createParser(longCode.toString());
        parser.parse();
        long duration = new Date().getTime()-begin;
        long memory = (rt.totalMemory() - rt.freeMemory() - beginMemory)/(1024*1024);
        assertEquals(33, parser.maxStackSize());//maximum online stacks
        assertEquals(180001, parser.totalStackSize());

    }

    /**
     * msll paper: section 7 case 3
     */
    @Test
    void test_flat() {
        MsllParser<?> parser = builder.createParser("let a=100;");
        for (int i = 0; i < 1999; i++) {
            parser.parse();
        }
        ParserTree tree = parser.parse();
        assertEquals(1, parser.maxStackSize());//same as ll(1)
        assertEquals(1, parser.totalStackSize());//same as ll(1)

        StringBuilder longCode = new StringBuilder();
        //10k tokens
        for (int i=0; i<100; i++){
            longCode.append("let a=100;");
        }
        long begin = new Date().getTime();
        parser = builder.createParser(longCode.toString());
        Runtime rt = Runtime.getRuntime();
        long beginMemory = rt.totalMemory() - rt.freeMemory();
        parser.parse();
        long duration = new Date().getTime()-begin;
        long memory = (rt.totalMemory() - rt.freeMemory() - beginMemory)/(1024*1024);
        assertEquals(1, parser.maxStackSize());//same as ll(1)
        assertEquals(1, parser.totalStackSize());//same as ll(1)
    }

    @Test
    void test(){
        ParserTree tree = builder.createParser("a.b").parse();
        assertEquals(1,1);
    }

}
