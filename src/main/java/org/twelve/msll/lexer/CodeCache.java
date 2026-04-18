package org.twelve.msll.lexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates source lines for the line-oriented {@link RegexLexer}.
 *
 * <p>Most grammars are line-local: one logical line produces a self-contained
 * list of tokens. A small but important class of grammars uses tokens that
 * span multiple physical lines: C-style block comments ({@code /* ... *}{@code /}),
 * Python / Kotlin / Scala triple-quoted strings ({@code """..."""} and
 * {@code '''...'''}), and similar. For those, this cache joins consecutive
 * physical lines with a literal {@code \n} into one logical line, which the
 * lexer then scans as a single unit.
 *
 * <p>The set of recognised opener/closer pairs lives in {@link #DELIMITERS}.
 * Each pair is scanned left-to-right, honouring nesting within a single line
 * (e.g. {@code /* a *}{@code /} closes on the same line and does not trigger
 * multi-line mode).
 *
     * <p>All incoming physical lines are trimmed to match the legacy
     * {@code line.trim()} behaviour of the pre-PR-2 cache. This means the
     * per-line indentation inside a multi-line literal is lost &mdash; an
     * explicit non-goal for PR-2, whose focus is making multi-line tokens
     * <em>reach the parser at all</em>. Full whitespace fidelity belongs in a
     * future, indentation-aware pass.
 */
public class CodeCache {

    private static final class Pair {
        final String open;
        final String close;
        Pair(String open, String close) { this.open = open; this.close = close; }
    }

    /** Ordered so that longer openers are tried first (e.g. "\"\"\"" before "\""). */
    private static final List<Pair> DELIMITERS = Collections.unmodifiableList(Arrays.asList(
            new Pair("\"\"\"", "\"\"\""),
            new Pair("'''", "'''"),
            new Pair("/*", "*/")
    ));

    private final List<String> lines = new ArrayList<>();

    /** Non-null when a multi-line token started on some previous line and still expects this closer. */
    private String pendingCloser = null;

    public String getLine(int lineNum) {
        if (lineNum >= this.lines.size()) {
            return "";
        }
        return this.lines.get(lineNum);
    }

    public int addLine(String line) {
        String trimmed = line.trim();
        if (this.pendingCloser != null) {
            String prev = this.lines.remove(this.lines.size() - 1);
            int suffixStart = prev.length() + 1; // +1 for the '\n' separator
            String joined = prev + "\n" + trimmed;
            this.lines.add(joined);
            this.pendingCloser = scanForPendingCloser(joined, suffixStart, this.pendingCloser);
        } else {
            this.lines.add(trimmed);
            this.pendingCloser = scanForPendingCloser(trimmed, 0, null);
        }
        return this.lines.size() - 1;
    }

    public boolean isMultiLine() {
        return this.pendingCloser != null;
    }

    /**
     * Walk {@code text} from {@code from} and return the closer that is still
     * open at end-of-string, or {@code null} if everything balances.
     *
     * @param openCloser if non-null, the text is assumed to start already inside
     *                   a multi-line token waiting for this closer.
     */
    private static String scanForPendingCloser(String text, int from, String openCloser) {
        int i = from;
        String waiting = openCloser;
        while (i < text.length()) {
            if (waiting != null) {
                int idx = text.indexOf(waiting, i);
                if (idx < 0) return waiting;
                i = idx + waiting.length();
                waiting = null;
                continue;
            }
            Pair opened = null;
            int openAt = -1;
            for (Pair p : DELIMITERS) {
                int at = text.indexOf(p.open, i);
                if (at >= 0 && (openAt < 0 || at < openAt)) {
                    opened = p;
                    openAt = at;
                }
            }
            if (opened == null) return null;
            waiting = opened.close;
            i = openAt + opened.open.length();
        }
        return waiting;
    }
}
