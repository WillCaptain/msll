package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammar.ParserTreeGrammarBuilder;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parsetree.LexerRuleTree;
import org.twelve.msll.parsetree.ParserGrammarTree;
import org.twelve.msll.util.Tool;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * MyParserBuilder is responsible for constructing the parser for a custom language.
 *
 * This builder uses the `ParserTreeGrammarBuilder` to convert the parser and lexer grammar trees
 * (created by `ParserGrammarParser` and `LexerGrammarParser`) into a CFG grammar format. The constructed parser
 * is then capable of parsing a custom language based on these grammar trees.
 *
 * It provides several constructors to initialize the parser builder using either file paths or `Reader` objects
 * to load the grammar definitions for both the parser and the lexer.
 *
 * @author huizi 2024
 */
public class MyParserBuilder extends ParserBuilder<ParserTreeGrammarBuilder, MyParser> {
    /**
     * Parse tree created by the ParserGrammarParser, representing the parser's grammar rules
     */
    private final ParserGrammarTree parserGrammarTree;
    /**
     * Parse tree created by the LexerRuleParser, representing the lexer's grammar rules
     */
    private final LexerRuleTree lexerRuleTree;

    /**
     * Private constructor that initializes the builder with parsed grammar trees.
     *
     * This constructor is used internally to initialize the builder with a `ParserGrammarTree` and
     * a `LexerRuleTree`, which are both generated from the parser and lexer grammar files. These trees
     * define the grammar structure of the custom language and are converted to CFG format by
     * `ParserTreeGrammarBuilder`.
     *
     * @param parserGrammarTree The parsed grammar tree for the parser.
     * @param lexerRuleTree The parsed grammar tree for the lexer.
     */
    private MyParserBuilder(ParserGrammarTree parserGrammarTree, LexerRuleTree lexerRuleTree) {
        super(new ParserTreeGrammarBuilder(parserGrammarTree, lexerRuleTree, NonTerminals.my(), Terminals.my()));
        this.parserGrammarTree = parserGrammarTree;
        this.lexerRuleTree = lexerRuleTree;
    }

    /**
     * Public constructor that initializes the builder using file paths for the parser and lexer grammar files.
     *
     * This constructor reads the grammar files for the custom language from the given file paths and
     * creates the corresponding grammar trees using `ParserGrammarParserBuilder` and `LexerRuleParserBuilder`.
     *
     * @param parserPath Path to the parser grammar file.
     * @param lexerPath Path to the lexer grammar file.
     * @throws IOException If there is an issue reading the files.
     */
    public MyParserBuilder(String parserPath, String lexerPath) throws IOException {
        this(new ParserGrammarParserBuilder().createParser(new FileReader(Tool.getGrammarFilePath(parserPath))).parse(),
                new LexerRuleParserBuilder().createParser(new FileReader(Tool.getGrammarFilePath(lexerPath))).parse());
    }

    /**
     * Public constructor that initializes the builder using `Reader` objects for the parser and lexer grammar files.
     *
     * This constructor allows for greater flexibility by accepting `Reader` objects as input, enabling
     * the grammar definitions to be provided from sources other than files, such as network streams or
     * in-memory strings.
     *
     * @param parserReader Reader for the parser grammar file.
     * @param lexerReader Reader for the lexer grammar file.
     */
    public MyParserBuilder(Reader parserReader, Reader lexerReader) {
        this(new ParserGrammarParserBuilder().createParser(parserReader).parse(), new LexerRuleParserBuilder().createParser(lexerReader).parse());
    }

    /**
     * Creates a new instance of `MyParser` using the initialized grammar structures.
     *
     * This method constructs a `MyParser` that is capable of parsing input according to the custom
     * language's grammar rules (both for the parser and the lexer) that have been defined and built into CFG format.
     *
     * @param reader The input source to be parsed by the custom parser.
     * @return A new `MyParser` instance for parsing the custom language.
     */
    @Override
    public MyParser createParser(Reader reader) {
        return new MyParser(this.grammars, this.predictTable, this.nonTerminals, this.terminals, reader);
    }

    /**
     * Returns the `ParserGrammarTree` which contains the grammar structure for the parser.
     *
     * @return The parsed grammar tree for the parser.
     */
    public ParserGrammarTree parserGrammarTree() {
        return this.parserGrammarTree;
    }

    /**
     * Returns the `LexerRuleTree` which contains the grammar structure for the lexer.
     *
     * @return The parsed grammar tree for the lexer.
     */
    public LexerRuleTree lexerGrammarTree(){
        return this.lexerRuleTree;
    }
}
