package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammar.CfgGrammarBuilder;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.ParserGrammarParser;

import java.io.Reader;

/**
 * ParserGrammarParserBuilder is responsible for constructing a parser specifically for
 * parsing G4 parser grammar files.
 * this parser builder user cfg grammar build to build the grammar
 *
 * This class extends the abstract `ParserBuilder` and provides the specific configuration
 * for parsing G4 files that define grammar rules. It uses a `CfgGrammarBuilder` initialized with
 * hardcoded grammar rules for G4 parser grammar and builds a corresponding `ParserGrammarParser`.
 *
 * The built parser is designed to process the structure of G4 grammar definitions, allowing
 * them to be interpreted and converted into CFG rules.
 *
 * @author huizi 2024
 */
public class ParserGrammarParserBuilder extends ParserBuilder<CfgGrammarBuilder, ParserGrammarParser> {
    /**
     * Constructor that initializes the `ParserGrammarParserBuilder` with hardcoded G4 grammar rules.
     *
     * The constructor calls the parent class `ParserBuilder` with a new instance of `CfgGrammarBuilder`,
     * passing in an array of grammar rules that define the structure of G4 parser grammar. These rules
     * are responsible for handling G4-specific constructs such as `options`, `channels`, and `productions`.
     *
     * The grammar lines follow the G4 format, which includes non-terminals like `grammar`, `productions`,
     * and `lexer_command`. The provided grammar captures the syntax structure needed to parse G4 grammar files.
     */
    ParserGrammarParserBuilder() {
        super(new CfgGrammarBuilder(new String[]{
                "parser->comments' parser_head' comments' channel_options' grammars",
                "comments'->ε|line_comment' comments'",
                "line_comment'->LONG_COMMENT|COMMENT",
                "parser_head'->ε|parser_head",
                "parser_head->PARSER_GRAMMAR id';",
                "id'->ID | UPPER_ID",
                "channel_options'->ε|options_statement channel'|channel_statement options'",
                "options'->ε|options_statement",
                "options_statement->OPTIONS LEFT_BRACE option_list' RIGHT_BRACE",
                "option_list'->ε|option option_list'",
                "option->id' EQUAL id';",
                "channel'->ε|channel_statement",
                "channel_statement->CHANNELS LEFT_BRACE id' ids' RIGHT_BRACE",
                "ids'->ε| COMMA id' ids'",
                "grammars->grammar' more_grammar'",
                "more_grammar'->ε|grammar' more_grammar'",
                "grammar' -> grammar | line_comment'",
                "grammar->head' tail_comment' : productions lexer_command';",
                "lexer_command'->ε|LEXER_COMMAND",
                "tail_comment'->ε|COMMENT tail_comment'",
                "head'->non_terminal|terminal|fragment_terminal",
                "non_terminal->ID|<ID>",
                "terminal->UPPER_ID",
                "fragment_terminal->FRAGMENT UPPER_ID",
                "productions->production more_productions'",
                "more_productions'-> ε| production' more_productions'",
                "production'->COMMENT | OR production",
                "production-> associate symbol' more_production'",
                "associate->ε|LESS ASSOC EQUAL direct' GREATER",
                "direct'->NONE | LEFT | RIGHT",
                "more_production'-> ε | EXPLAIN | production",
                "symbol'-> symbol|zero_more|one_more|zero_one",
                "symbol-> STRING|REGEX|COMMENT|PREDICATE|non_terminal|terminal|factor",
                "factor-> LEFT_PAREN productions RIGHT_PAREN",
                "zero_more-> symbol STAR ",
                "one_more-> symbol PLUS ",
                "zero_one-> symbol QUESTION "
        }, NonTerminals.parser(),Terminals.parser()));
    }

    @Override
    public ParserGrammarParser createParser(Reader reader) {
        return new ParserGrammarParser(this.grammars, this.predictTable,this.nonTerminals,this.terminals,reader);
    }

}
