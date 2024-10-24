package org.twelve.msll;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.lexer.Lexer;
import org.twelve.msll.lexer.RegexLexer;
import org.twelve.msll.lexer.TokenBuffer;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.Tool;

import java.io.FileReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


class RegexLexerTest {
    private final static String TEST_GRAMMAR = "simpleExpressionParser.gm";
    private final static String JS_GRAMMAR = "javascriptParser.gm";
    @BeforeEach
    @SneakyThrows
    void setup() {
    }

    @SneakyThrows
    private Lexer createStringLexer(String code){
        return new RegexLexer(new StringReader(code), Terminals.parser());
    }
    @SneakyThrows
    private Lexer createFileLexer(String fileName){
        return new RegexLexer(new FileReader(Tool.getGrammarFilePath(fileName)),Terminals.parser());
    }
    @Test
    @SneakyThrows
    void test_tokenize_one_grammar(){
        TokenBuffer tokens = createStringLexer("script : import_declares statements export_declare?;").scan();
        wait(tokens);
        assertEquals(9,tokens.size());
        assertEquals("ID",tokens.get(0).terminal().name());
        assertEquals("script",tokens.get(0).lexeme());

        assertEquals("COLON",tokens.get(1).terminal().name());
        assertEquals(":",tokens.get(1).lexeme());

        assertEquals(Constants.EOL_STR,tokens.get(7).toString());
        assertEquals("\\$",tokens.get(8).lexeme());
    }

    @SneakyThrows
    private static void wait(TokenBuffer tokens) {
        int timeout = 0;
        while(tokens.size()==0 || !tokens.get(tokens.size()-1).terminal().name().equals(Constants.END_STR)){
            Thread.sleep(10);
            if(++timeout==1000){
                fail();
            }
        }
    }


    @Test
    @SneakyThrows
    void test_tokenize_long_comments() {
        TokenBuffer tokens = createStringLexer("b:ID PLUS b;\n /*comments\n comments\n*/\n a:ID PLUS a;").scan();
        wait(tokens);
        assertEquals(17,tokens.size());
        assertEquals(Constants.LONG_COMMENT,tokens.get(7).terminal().name());
    }

    @Test
    @SneakyThrows
    void test_tokenize_test_grammar_file() {
        TokenBuffer tokens = createFileLexer(TEST_GRAMMAR).scan();
        wait(tokens);
        assertEquals(104,tokens.size());
    }

    @Test
    @SneakyThrows
    void test_tokenize_js_grammar_file() {
        TokenBuffer tokens = createFileLexer(JS_GRAMMAR).scan();
        wait(tokens);
        assertEquals(1838,tokens.size());
    }
}
