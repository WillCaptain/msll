package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammar.CfgGrammarBuilder;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.LexerRuleParser;

import java.io.Reader;

/**
 * LexerRuleParserBuilder is responsible for constructing a parser specifically for
 * parsing G4 lexer rule grammar files.
 * this parser builder user cfg grammar build to build the grammar
 *
 * This class extends `ParserBuilder` and provides the configuration necessary
 * for parsing G4 files that define lexer rules. It uses a `CfgGrammarBuilder`, initialized with
 * hardcoded grammar rules for G4 lexer grammar, to build a `LexerRuleParser`.
 *
 * The built parser processes the structure of G4 lexer definitions, enabling the recognition
 * of lexer rules such as regular expressions, terminals, and lexer commands.
 *
 * @author huizi 2024
 */
public class LexerRuleParserBuilder extends ParserBuilder<CfgGrammarBuilder, LexerRuleParser> {
    /**
     * Constructor that initializes the `LexerRuleParserBuilder` with hardcoded G4 lexer grammar rules.
     *
     * The constructor calls the parent class `ParserBuilder` with a new instance of `CfgGrammarBuilder`,
     * passing an array of grammar rules that define the structure of G4 lexer rules. These rules
     * handle the different components of lexer grammar files, such as terminals, productions,
     * channels, and lexer commands.
     *
     * The grammar rules provided are specific to G4 lexer grammar files, enabling the recognition of G4 lexer structures.
     */
    public LexerRuleParserBuilder() {
        super(new CfgGrammarBuilder(new String[]{
                "lexer->comments' lexer_head' comments' channel_options' grammars",
                "comments'->ε|line_comment' comments'",
                "line_comment'->LONG_COMMENT|COMMENT",
                "lexer_head'->ε|lexer_head",
                "lexer_head->LEXER_GRAMMAR UPPER_ID;",
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
                "grammar->terminal tail_comment' : productions lexer_command';",
                "lexer_command'->ε|LEXER_COMMAND",
                "tail_comment'->ε|COMMENT tail_comment'",
                "terminal->UPPER_ID",
                "productions->production more_productions'",
                "more_productions'-> ε| production' more_productions'",
                "production'->COMMENT | OR production",
                "production-> associate symbol more_production'",
                "associate->ε|LESS ASSOC EQUAL direct' GREATER",
                "direct'->NONE | LEFT | RIGHT",
                "more_production'-> ε | EXPLAIN | production",
                "symbol-> STRING|REGEX|COMMENT|PREDICATE|terminal",

        }, NonTerminals.lexer(), Terminals.lexer()));
    }

    @Override
    public LexerRuleParser createParser(Reader reader) {
        return new LexerRuleParser(this.grammars, this.predictTable,this.nonTerminals,this.terminals,reader);
    }
}
