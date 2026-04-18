package org.twelve.msll.tools;

import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Single entry point for loading an ANTLR4 {@code .g4} grammar into MSLL.
 *
 * <p>Callers should not have to know about the internal conversion pipeline;
 * they give us a {@code .g4} source (either a combined grammar or a
 * (lexer, parser) pair) and get back a ready-to-use {@link MyParserBuilder}.
 *
 * <p>The pipeline, in order, is:
 * <ol>
 *   <li>{@link G4Splitter#split} &mdash; handles combined grammars by
 *       splitting them into lexer and parser halves based on rule name case.</li>
 *   <li>{@link G4ImplicitTokens#lift} &mdash; promotes anonymous string
 *       literals in parser rules into named lexer rules, because MSLL only
 *       accepts declared terminals.</li>
 *   <li>{@link G4ToGMConverter#convert} &mdash; applies the small syntactic
 *       rewrites needed to move from ANTLR4's {@code .g4} dialect to MSLL's
 *       {@code .gm} dialect (drop {@code EOF}, rename the start rule to
 *       {@code root}, strip empty alternatives, etc).</li>
 *   <li>Construct a {@link MyParserBuilder} from the two converted sources.</li>
 * </ol>
 *
 * <p>Every stage is re-usable on its own; this class owns nothing but the
 * wiring.
 */
public final class G4GrammarLoader {

    private G4GrammarLoader() {}

    /**
     * Converted intermediate representation plus the ready-to-use builder.
     * Exposing the {@code .gm} strings makes it trivial to diff against the
     * original grammar when debugging a conversion failure &mdash; tests use
     * this to dump to {@code target/gm-dump/...}.
     */
    public static final class Loaded {
        public final MyParserBuilder builder;
        public final String lexerGm;
        public final String parserGm;
        public final int liftedCount;

        Loaded(MyParserBuilder builder, String lexerGm, String parserGm, int liftedCount) {
            this.builder = builder;
            this.lexerGm = lexerGm;
            this.parserGm = parserGm;
            this.liftedCount = liftedCount;
        }
    }

    /**
     * Loads a combined grammar (a single {@code grammar X;} file). If the
     * file is actually lexer-only or parser-only the splitter will detect
     * that and leave the other half empty, which will likely fail at
     * {@link MyParserBuilder} construction &mdash; in that case use
     * {@link #loadG4(Path, Path)}.
     */
    public static Loaded loadG4(Path combinedG4) throws IOException {
        return loadG4String(Files.readString(combinedG4), null);
    }

    /** Loads a split grammar from two files. */
    public static Loaded loadG4(Path lexerG4, Path parserG4) throws IOException {
        return loadG4String(Files.readString(lexerG4), Files.readString(parserG4));
    }

    /**
     * Loads a grammar from raw source strings.
     *
     * @param lexerG4 raw lexer {@code .g4} source, or {@code null} if
     *                {@code parserG4} is actually a combined grammar and
     *                should be auto-split.
     * @param parserG4 raw parser {@code .g4} source, or &mdash; when
     *                 {@code lexerG4} is {@code null} &mdash; the whole
     *                 combined grammar.
     */
    public static Loaded loadG4String(String lexerG4, String parserG4) {
        // Normalise the inputs to a (lexer, parser) pair.
        String lexerSrc;
        String parserSrc;
        if (lexerG4 == null) {
            G4Splitter.Split s = G4Splitter.split(parserG4, null);
            lexerSrc = s.lexer;
            parserSrc = s.parser;
        } else {
            lexerSrc = lexerG4;
            parserSrc = parserG4;
        }

        // Lift implicit literals before the .gm syntax conversion so that
        // the converter only sees declared terminals.
        G4ImplicitTokens.Result lifted = G4ImplicitTokens.lift(lexerSrc, parserSrc);

        String lexerGm = G4ToGMConverter.convert(lifted.lexer, true);
        String parserGm = G4ToGMConverter.convert(lifted.parser, false);

        MyParserBuilder builder = new MyParserBuilder(
                new StringReader(parserGm), new StringReader(lexerGm));
        return new Loaded(builder, lexerGm, parserGm, lifted.liftedCount);
    }
}
