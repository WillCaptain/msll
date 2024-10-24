package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammar.GrammarBuilder;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.MsllParser;
import org.twelve.msll.parser.PredictTable;

import java.io.Reader;
import java.io.StringReader;

/**
 * Abstract ParserBuilder class responsible for building a parser based on the provided grammars.
 *
 * This class initializes the grammar structure (terminals, non-terminals, and production rules)
 * using the provided GrammarBuilder. It also builds the predict table necessary for parsing.
 *
 * @param <B> Type parameter representing the specific GrammarBuilder used.
 * @param <P> Type parameter representing the specific MsllParser used.
 *
 * @author huizi 2024
 */
public abstract class ParserBuilder<B extends GrammarBuilder, P extends MsllParser> {

    /**
     * The set of grammars constructed by the GrammarBuilder
     */
    protected final Grammars grammars;
    /**
     * The predict table used to match tokens to grammar rules
     */
    protected final PredictTable predictTable;
    /**
     * The set of non-terminal symbols used in the grammar
     */
    protected final NonTerminals nonTerminals;
    /**
     * The set of terminal symbols used in the grammar.
     */
    protected final Terminals terminals;

    /**
     * Constructor to initialize the ParserBuilder with the provided GrammarBuilder.
     *
     * The constructor initializes the terminals and non-terminals from the grammar builder,
     * builds the grammars, and constructs the predict table based on the provided grammar.
     *
     * @param grammarBuilder The GrammarBuilder used to define and construct the grammar.
     */
    public ParserBuilder(B grammarBuilder) {
        this.nonTerminals = grammarBuilder.nonTerminals();
        this.terminals = grammarBuilder.terminals();
        initProductions(grammarBuilder);
        this.grammars = grammarBuilder.build();
        this.predictTable = new PredictTable(this.grammars);
    }

    /**
     * Initializes the production rules using the provided GrammarBuilder.
     *
     * This method invokes the `initialize()` method of the GrammarBuilder to ensure that the grammar
     * structure is properly set up before parsing.
     *
     * @param grammarBuilder The GrammarBuilder used to initialize the productions.
     */
    protected void initProductions(B grammarBuilder){
        grammarBuilder.initialize();
    }

    /**
     * Retrieves the current set of grammars being used by the parser.
     *
     * @return The set of grammars constructed by the GrammarBuilder.
     */
    public Grammars grammars() {
        return this.grammars;
    }

    /**
     * Creates a new parser instance using the given Reader input.
     *
     * This method must be implemented by subclasses to create a parser that can parse
     * input from a given source.
     *
     * @param reader The input source to be parsed by the parser.
     * @return A new parser instance capable of parsing the input.
     */
    public abstract P createParser(Reader reader);
    /**
     * Creates a new parser instance using a string input.
     *
     * This method wraps the string in a StringReader and calls the main `createParser` method.
     * It provides a convenient way to create a parser for string-based input.
     *
     * @param code The string input to be parsed.
     * @return A new parser instance capable of parsing the input.
     */
    public P createParser(String code){
        return this.createParser(new StringReader(code));
    }

    /**
     * Retrieves the terminal symbols used in the grammar.
     *
     * @return The set of terminal symbols used in the parser's grammar.
     */
    public Terminals terminals(){
        return this.terminals;
    }
    /**
     * Retrieves the non-terminal symbols used in the grammar.
     *
     * @return The set of non-terminal symbols used in the parser's grammar.
     */
    public NonTerminals nonTerminals(){
        return this.nonTerminals;
    }
}
