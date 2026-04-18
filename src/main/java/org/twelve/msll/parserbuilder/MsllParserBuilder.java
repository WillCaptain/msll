package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammar.ParserTreeGrammarBuilder;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parser.PredictTable;
import org.twelve.msll.parsetree.LexerRuleTree;
import org.twelve.msll.parsetree.ParserGrammarTree;

import java.io.Reader;

/**
 * Builds an MSLL runtime parser from a {@code (parser.gm, lexer.gm)} pair
 * using <em>only</em> the structural built-in terminals seeded by
 * {@link Terminals#newBare()}.
 *
 * <p>This is the entry point used by the {@code G4GrammarLoader} pipeline.
 * Every token consumed by the resulting parser must originate in the user
 * grammar's own lexer rules &mdash; there is no Outline-language token table
 * (no built-in {@code STRING}, {@code COMMA}, {@code ++}, {@code ==}, etc.)
 * silently competing with the user's declarations.
 *
 * <p>For the Outline language itself, use {@link MyParserBuilder}, which
 * extends this class and additionally seeds the table via
 * {@link Terminals#newMy()}.
 *
 * <p>Both builders return the same {@link MyParser} runtime class. The
 * "MyParser" name is historical; the runtime is grammar-agnostic and the
 * Outline-specific tweaks it carries
 * ({@code factor_expression_alpha'} epsilon override, {@code "statement"}
 * panic-recovery hint) are no-ops on grammars that do not declare those
 * non-terminals.
 *
 * @author huizi 2024
 */
public class MsllParserBuilder extends ParserBuilder<ParserTreeGrammarBuilder, MyParser> {

    private final ParserGrammarTree parserGrammarTree;
    private final LexerRuleTree lexerRuleTree;

    /**
     * Internal constructor: hands fully-parsed grammar trees and the seed
     * symbol tables to the {@link ParserTreeGrammarBuilder}. Subclasses
     * (e.g. {@link MyParserBuilder}) call this with their own seeds.
     */
    protected MsllParserBuilder(ParserGrammarTree parserGrammarTree,
                                LexerRuleTree lexerRuleTree,
                                NonTerminals nonTerminals,
                                Terminals terminals) {
        super(new ParserTreeGrammarBuilder(parserGrammarTree, lexerRuleTree,
                nonTerminals, terminals));
        this.parserGrammarTree = parserGrammarTree;
        this.lexerRuleTree = lexerRuleTree;
    }

    /**
     * Public bare entry: parses the two grammar sources and seeds with
     * {@link Terminals#newBare()} / {@link NonTerminals#newMy()}. The
     * latter is already a non-shared, non-Outline-specific factory.
     */
    public MsllParserBuilder(Reader parserReader, Reader lexerReader) {
        this(new ParserGrammarParserBuilder().createParser(parserReader).parse(),
             new LexerRuleParserBuilder().createParser(lexerReader).parse(),
             NonTerminals.newMy(),
             Terminals.newBare());
    }

    @Override
    public MyParser createParser(Reader reader) {
        return new MyParser(this.grammars, this.predictTable,
                this.nonTerminals, this.terminals, reader);
    }

    /**
     * Exposes the internal {@link PredictTable} so the G4 loader can flip on
     * ANTLR4-style FIRST/FOLLOW conflict handling
     * ({@code setAutoEpsilonAlongsideEnabled(true)}). Outline callers
     * normally leave it untouched.
     */
    public PredictTable predictTable() {
        return this.predictTable;
    }

    /** Returns the parsed parser-grammar tree (debugging / inspection). */
    public ParserGrammarTree parserGrammarTree() {
        return this.parserGrammarTree;
    }

    /** Returns the parsed lexer-grammar tree (debugging / inspection). */
    public LexerRuleTree lexerGrammarTree() {
        return this.lexerRuleTree;
    }
}
