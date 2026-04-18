package org.twelve.msll.grammarsv4;

import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parsetree.ParserTree;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compatibility harness for ANTLR4 grammars-v4 ({@code .g4} files).
 *
 * <p>All grammar-conversion logic lives in {@link G4GrammarLoader}; this
 * class only adds stage-classified error reporting, per-sample parsing, and
 * a Markdown-rendered summary suitable for the paper's RQ1 table. Every
 * stage is wrapped so a failure is captured as a categorised {@link Outcome}
 * rather than propagating an exception &mdash; red runs still produce a
 * complete report.
 *
 * <p>Non-JUnit by design so it can be reused by both
 * {@link GrammarsV4CompatTest} and a future CLI {@code main(...)} entry.
 */
public class GrammarsV4CompatRunner {

    /** Single grammar under test. */
    public static class Case {
        /** Human-readable label used in the report (e.g. "json", "java"). */
        public final String name;
        /**
         * One or two {@code .g4} files. A single entry is assumed to be either
         * a combined {@code grammar X;} (which will be auto-split) or a
         * self-contained lexer/parser grammar. Two entries are treated as
         * (lexer, parser) in that order.
         */
        public final List<Path> g4Files;
        /** Zero or more sample input files that should parse successfully. */
        public final List<Path> samples;

        public Case(String name, List<Path> g4Files, List<Path> samples) {
            this.name = name;
            this.g4Files = g4Files;
            this.samples = samples;
        }

        public static Case combined(String name, Path g4, List<Path> samples) {
            return new Case(name, List.of(g4), samples);
        }

        public static Case split(String name, Path lexerG4, Path parserG4, List<Path> samples) {
            return new Case(name, Arrays.asList(lexerG4, parserG4), samples);
        }
    }

    /** Coarse classification of where a grammar failed (or succeeded). */
    public enum Stage {
        /** Unable to read the {@code .g4} source from disk. */
        READ,
        /** {@link G4GrammarLoader} rejected the grammar before builder construction. */
        LOAD,
        /** Grammar loaded, but at least one sample input failed to parse. */
        PARSE,
        /** Grammar loaded and all samples parsed without error. */
        OK
    }

    /** Outcome of running one {@link Case}. */
    public static class Outcome {
        public final String name;
        public final Stage stage;
        public final String detail;
        public final int samplesTotal;
        public final int samplesOk;
        public final long loadMillis;
        public final long parseMillisTotal;

        public Outcome(String name, Stage stage, String detail,
                       int samplesTotal, int samplesOk,
                       long loadMillis, long parseMillisTotal) {
            this.name = name;
            this.stage = stage;
            this.detail = detail;
            this.samplesTotal = samplesTotal;
            this.samplesOk = samplesOk;
            this.loadMillis = loadMillis;
            this.parseMillisTotal = parseMillisTotal;
        }

        public boolean ok() { return stage == Stage.OK; }
    }

    /** Runs {@code c} end-to-end, never throwing. */
    public static Outcome run(Case c) {
        // --- stage 1: read ----------------------------------------------------
        String lexerG4;
        String parserG4;
        try {
            if (c.g4Files.size() == 1) {
                // Combined grammar; G4GrammarLoader will auto-split.
                lexerG4 = null;
                parserG4 = Files.readString(c.g4Files.get(0));
            } else if (c.g4Files.size() == 2) {
                lexerG4 = Files.readString(c.g4Files.get(0));
                parserG4 = Files.readString(c.g4Files.get(1));
            } else {
                return new Outcome(c.name, Stage.READ,
                        "expected 1 or 2 .g4 files, got " + c.g4Files.size(),
                        c.samples.size(), 0, 0, 0);
            }
        } catch (IOException e) {
            return new Outcome(c.name, Stage.READ, "read failed: " + e.getMessage(),
                    c.samples.size(), 0, 0, 0);
        }

        // --- stage 2: load (split + lift + convert + build) -------------------
        G4GrammarLoader.Loaded loaded;
        long loadStart = System.nanoTime();
        try {
            loaded = G4GrammarLoader.loadG4String(lexerG4, parserG4);
        } catch (Throwable t) {
            return new Outcome(c.name, Stage.LOAD, firstLine(t),
                    c.samples.size(), 0, msSince(loadStart), 0);
        }
        long loadMs = msSince(loadStart);

        // Optionally dump the converted grammars for offline debugging.
        // Enable with -Dmsll.compat.dump=target/gm-dump
        String dump = System.getProperty("msll.compat.dump");
        if (dump != null && !dump.isBlank()) {
            try {
                Path dir = Path.of(dump, c.name);
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("lexer.gm"), loaded.lexerGm);
                Files.writeString(dir.resolve("parser.gm"), loaded.parserGm);
            } catch (IOException ignore) { /* debugging aid only */ }
        }

        // --- stage 4: parse ---------------------------------------------------
        long parseMsTotal = 0;
        int okCount = 0;
        StringBuilder parseErr = new StringBuilder();
        for (Path sample : c.samples) {
            String text;
            try {
                text = Files.readString(sample);
            } catch (IOException e) {
                parseErr.append(sample.getFileName()).append(": read: ")
                        .append(e.getMessage()).append('\n');
                continue;
            }
            long t = System.nanoTime();
            try {
                MyParser parser = loaded.builder.createParser(new StringReader(text));
                ParserTree tree = parser.parse();
                if (tree == null) {
                    parseErr.append(sample.getFileName()).append(": null tree\n");
                } else {
                    okCount++;
                }
            } catch (Throwable t2) {
                parseErr.append(sample.getFileName()).append(": ")
                        .append(firstLine(t2)).append('\n');
            }
            parseMsTotal += (System.nanoTime() - t) / 1_000_000;
        }

        Stage stage = (okCount == c.samples.size()) ? Stage.OK : Stage.PARSE;
        String detail = stage == Stage.OK ? "" : parseErr.toString().trim();
        return new Outcome(c.name, stage, detail,
                c.samples.size(), okCount, loadMs, parseMsTotal);
    }

    /** Convenience: run a batch of cases and collect results. */
    public static List<Outcome> runAll(List<Case> cases) {
        List<Outcome> out = new ArrayList<>(cases.size());
        for (Case c : cases) out.add(run(c));
        return out;
    }

    /** Renders outcomes as a GitHub-flavoured Markdown table (paper-ready). */
    public static String renderMarkdown(List<Outcome> outcomes) {
        StringBuilder sb = new StringBuilder();
        sb.append("| Grammar | Stage | Samples | Load (ms) | Parse (ms) | Detail |\n");
        sb.append("|---|---|---|---|---|---|\n");
        for (Outcome o : outcomes) {
            sb.append("| ").append(o.name)
                    .append(" | ").append(o.stage)
                    .append(" | ").append(o.samplesOk).append('/').append(o.samplesTotal)
                    .append(" | ").append(o.loadMillis)
                    .append(" | ").append(o.parseMillisTotal)
                    .append(" | ").append(escape(o.detail))
                    .append(" |\n");
        }
        int ok = (int) outcomes.stream().filter(Outcome::ok).count();
        sb.append("\n**Summary:** ").append(ok).append('/').append(outcomes.size())
                .append(" grammars fully compatible.\n");
        return sb.toString();
    }

    private static String firstLine(Throwable t) {
        String msg = t.getClass().getSimpleName();
        if (t.getMessage() != null) {
            String m = t.getMessage().replace('\n', ' ').trim();
            if (m.length() > 120) m = m.substring(0, 120) + "...";
            msg += ": " + m;
        }
        return msg;
    }

    private static String escape(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replace("|", "\\|").replace("\n", "<br>");
    }

    private static long msSince(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000;
    }
}
