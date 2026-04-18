package org.twelve.msll.parserbuilder;

import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.parsetree.LexerRuleTree;
import org.twelve.msll.parsetree.ParserGrammarTree;
import org.twelve.msll.util.Tool;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Outline-flavoured wrapper around {@link MsllParserBuilder}.
 *
 * <p>This class adds the Outline language's built-in token vocabulary
 * (string literals, {@code ++}/{@code --}/{@code ==}/etc., punctuation,
 * {@code FLOAT} and {@code DOUBLE} numeric forms, ...) on top of the
 * structural built-ins. Use it whenever the parser is expected to handle
 * Outline-style source.
 *
 * <p>For grammars loaded from external {@code .g4} sources, prefer the
 * bare {@link MsllParserBuilder} so that Outline tokens do not pollute
 * the user grammar's terminal table. The G4 loader does this automatically.
 *
 * <p>The public constructor signatures and the {@code createParser} return
 * type are kept identical to the pre-extraction version to avoid any
 * change to existing call sites.
 *
 * @author huizi 2024
 */
public class MyParserBuilder extends MsllParserBuilder {

    private MyParserBuilder(ParserGrammarTree parserGrammarTree, LexerRuleTree lexerRuleTree) {
        // Outline seeds: a fresh NonTerminals plus the full Outline token table
        // (Terminals.newMy). Fresh instances per build avoid cross-grammar leak
        // of registered lexer rules - see Terminals#newMy / NonTerminals#newMy.
        super(parserGrammarTree, lexerRuleTree,
                NonTerminals.newMy(), Terminals.newMy());
    }

    /**
     * Reads grammar definitions from disk via the workspace-relative grammar
     * lookup helper. Equivalent to wrapping the files in {@link FileReader}s.
     */
    public MyParserBuilder(String parserPath, String lexerPath) throws IOException {
        this(new ParserGrammarParserBuilder()
                        .createParser(new FileReader(Tool.getGrammarFilePath(parserPath))).parse(),
             new LexerRuleParserBuilder()
                        .createParser(new FileReader(Tool.getGrammarFilePath(lexerPath))).parse());
    }

    /**
     * Reads grammar definitions from arbitrary {@link Reader}s. Lets callers
     * provide grammar sources from network streams, in-memory strings, etc.
     */
    public MyParserBuilder(Reader parserReader, Reader lexerReader) {
        this(new ParserGrammarParserBuilder().createParser(parserReader).parse(),
             new LexerRuleParserBuilder().createParser(lexerReader).parse());
    }
}
