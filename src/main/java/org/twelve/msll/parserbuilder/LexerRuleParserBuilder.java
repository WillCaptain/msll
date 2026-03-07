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
                "lexer->comments' lexer_head' comments' channel_options' lex_sections'",
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
                "ids'->ε|COMMA id' ids'",
                // Sections: regular rules, fragment rules, mode declarations
                "lex_sections'->ε|lex_section' lex_sections'",
                "lex_section'->grammar|fragment_grammar|mode_decl|line_comment'",
                "mode_decl->MODE UPPER_ID;",
                // Regular lexer rule: NAME : body command? ;
                "grammar->terminal tail_comment' : lex_body lexer_command' ;",
                // Fragment rule: fragment NAME : body ;  (no lexer command)
                "fragment_grammar->FRAGMENT terminal tail_comment' : lex_body ;",
                "lexer_command'->ε|LEXER_COMMAND",
                "tail_comment'->ε|COMMENT tail_comment'",
                "terminal->UPPER_ID",
                // Lexer rule body: alternatives separated by |
                "lex_body->lex_alt lex_more_alts'",
                "lex_more_alts'->ε|OR lex_alt lex_more_alts'",
                // Single alternative: a sequence of elements
                "lex_alt->lex_elem lex_more_elems'",
                "lex_more_elems'->ε|lex_elem lex_more_elems'",
                // Element: atom with optional quantifier (greedy or lazy: *? +?)
                "lex_elem->lex_atom lex_quantifier'",
                "lex_quantifier'->ε|STAR lazy_q'|PLUS lazy_q'|QUESTION",
                "lazy_q'->ε|QUESTION",
                // Atom: terminal chars, char classes, regex, groups, negation, fragment refs
                "lex_atom->STRING|SINGLE_CHARACTER|ANY|SPECIAL|REGEX|PREDICATE|terminal|lex_group|lex_not",
                "lex_group->LEFT_PAREN lex_body RIGHT_PAREN",
                "lex_not->NOT lex_atom",
        }, NonTerminals.lexer(), Terminals.lexer()));
    }

    @Override
    public LexerRuleParser createParser(Reader reader) {
        return new LexerRuleParser(this.grammars, this.predictTable,this.nonTerminals,this.terminals,reader);
    }
}
