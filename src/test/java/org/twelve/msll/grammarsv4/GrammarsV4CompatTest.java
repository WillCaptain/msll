package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link GrammarsV4CompatRunner} against the vendored subset of
 * ANTLR's {@code grammars-v4} corpus under
 * {@code src/test/resources/grammars-v4/}.
 *
 * <p>Every sub-directory there is one grammar case:
 * <pre>
 *   grammars-v4/
 *     json/
 *       JSON.g4           ← combined grammar (auto-split)
 *       samples/
 *         simple.json
 *         nested.json
 *     ini/
 *       INILexer.g4       ← split grammars are used as-is
 *       INIParser.g4
 *       samples/...
 * </pre>
 *
 * <p>This test is intentionally <em>non-asserting by default</em> on
 * individual grammars: it collects outcomes, writes a Markdown report to
 * {@code target/grammars-v4-compat.md} and a human-readable summary to
 * stdout, and only fails if the directory setup is broken (no cases found
 * at all). That keeps the harness useful during the compatibility-gap
 * hunt: red runs are expected and informative, not spurious CI failures.
 */
public class GrammarsV4CompatTest {

    private static final String ROOT = "grammars-v4";

    @Test
    void vendored_grammars_v4_subset_compatibility_report() throws Exception {
        Path rootDir = resolveResourceRoot();
        List<GrammarsV4CompatRunner.Case> cases = discoverCases(rootDir);
        assertTrue(!cases.isEmpty(),
                "No grammars-v4 cases discovered under " + rootDir);

        List<GrammarsV4CompatRunner.Outcome> outcomes =
                GrammarsV4CompatRunner.runAll(cases);

        String md = GrammarsV4CompatRunner.renderMarkdown(outcomes);
        Path report = Path.of("target", "grammars-v4-compat.md");
        Files.createDirectories(report.getParent());
        Files.writeString(report, md);

        // Also dump to stdout for quick inspection while iterating.
        System.out.println();
        System.out.println("=== grammars-v4 compatibility ===");
        System.out.println(md);
        System.out.println("Report written to: " + report.toAbsolutePath());
    }

    private static Path resolveResourceRoot() throws URISyntaxException {
        URL url = GrammarsV4CompatTest.class.getClassLoader().getResource(ROOT);
        Objects.requireNonNull(url, "resource not found on classpath: " + ROOT);
        return Path.of(url.toURI());
    }

    /**
     * One sub-directory of {@code grammars-v4/} per grammar. The case's
     * {@code .g4} files are whatever lives directly in that sub-directory;
     * samples live in {@code <subdir>/samples/}.
     */
    private static List<GrammarsV4CompatRunner.Case> discoverCases(Path root) throws IOException {
        List<GrammarsV4CompatRunner.Case> cases = new ArrayList<>();
        try (Stream<Path> entries = Files.list(root)) {
            List<Path> dirs = entries.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            for (Path dir : dirs) {
                List<Path> g4s;
                try (Stream<Path> g = Files.list(dir)) {
                    g4s = g.filter(p -> p.getFileName().toString().endsWith(".g4"))
                            .sorted(lexerFirst())
                            .collect(Collectors.toList());
                }
                if (g4s.isEmpty()) continue;
                Path sampleDir = dir.resolve("samples");
                List<Path> samples = Files.isDirectory(sampleDir)
                        ? sampleFilesIn(sampleDir) : List.of();
                cases.add(new GrammarsV4CompatRunner.Case(
                        dir.getFileName().toString(), g4s, samples));
            }
        }
        return cases;
    }

    /** Puts {@code *Lexer.g4} before {@code *Parser.g4}; other names stay alphabetical. */
    private static Comparator<Path> lexerFirst() {
        return (a, b) -> {
            String an = a.getFileName().toString();
            String bn = b.getFileName().toString();
            int as = an.toLowerCase().contains("lexer") ? 0 : (an.toLowerCase().contains("parser") ? 2 : 1);
            int bs = bn.toLowerCase().contains("lexer") ? 0 : (bn.toLowerCase().contains("parser") ? 2 : 1);
            if (as != bs) return Integer.compare(as, bs);
            return an.compareTo(bn);
        };
    }

    private static List<Path> sampleFilesIn(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    /** Unused guard against IDEs rewriting imports. */
    @SuppressWarnings("unused")
    private static final List<?> KEEP = Arrays.asList();
}
