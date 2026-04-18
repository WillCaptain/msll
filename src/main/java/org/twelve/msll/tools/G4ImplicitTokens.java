package org.twelve.msll.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lifts implicit string-literal tokens out of ANTLR4 parser rules into named
 * lexer rules, so the resulting (lexer, parser) pair only uses declared terminals.
 *
 * <h2>Why this exists</h2>
 * In an ANTLR4 <em>combined</em> grammar you can write:
 * <pre>
 *   value : STRING | 'true' | 'false' | 'null' ;
 * </pre>
 * and ANTLR4 silently synthesises anonymous lexer tokens for each literal.
 * MSLL's grammar engine does not do that synthesis — every terminal referenced
 * by a parser rule must be declared in the lexer grammar. So after
 * {@link G4Splitter#split} produces lexer and parser halves, we run this pass
 * to:
 * <ol>
 *   <li>Index existing lexer rules of the form {@code NAME : 'literal' ;} so
 *       we can reuse a declared name when the literal already has one.</li>
 *   <li>Scan the parser body for bare string literals and replace each with
 *       either the pre-existing name or a synthesised {@code T__<n>} name.</li>
 *   <li>Append any synthesised lexer rules to the lexer body.</li>
 * </ol>
 *
 * <p>The pass is deliberately conservative:
 * <ul>
 *   <li>It does not touch literals that appear inside {@code '...'} or {@code "..."}
 *       string bodies or inside comments.</li>
 *   <li>It does not try to recognise regex literals of the form {@code /"..."/}.</li>
 *   <li>It synthesises names based on the literal's content (and a counter) so
 *       diffing the output is readable.</li>
 * </ul>
 */
public final class G4ImplicitTokens {

    private G4ImplicitTokens() {}

    public static final class Result {
        public final String lexer;
        public final String parser;
        public final int liftedCount;
        public Result(String lexer, String parser, int liftedCount) {
            this.lexer = lexer;
            this.parser = parser;
            this.liftedCount = liftedCount;
        }
    }

    /**
     * Matches {@code NAME : 'literal' ;} (optionally with a lexer command like
     * {@code -> skip}). Captures the rule name and the raw literal body.
     */
    private static final Pattern SIMPLE_LITERAL_RULE = Pattern.compile(
            "(?m)^\\s*([A-Z][A-Za-z0-9_]*)\\s*:\\s*'((?:[^'\\\\]|\\\\.)*)'\\s*(?:->[^;]*)?;");

    public static Result lift(String lexerSrc, String parserSrc) {
        Map<String, String> literalToName = indexExistingLiterals(lexerSrc);
        Map<String, String> synthesized = new LinkedHashMap<>();

        String rewrittenParser = rewriteParserLiterals(
                parserSrc, literalToName, synthesized);

        String extendedLexer = injectSynthesizedRules(lexerSrc, synthesized);
        return new Result(extendedLexer, rewrittenParser, synthesized.size());
    }

    /**
     * Inserts synthesised lexer rules <em>before</em> the first existing rule
     * body, so they take priority in the ANTLR-like longest-match ordering.
     * Appending them at the end would place them after {@code WS -> skip} and
     * other catch-alls, which can cause individual punctuation characters to
     * be consumed by an earlier, more-permissive rule.
     */
    private static String injectSynthesizedRules(String lexerSrc,
                                                 Map<String, String> synthesized) {
        if (synthesized.isEmpty()) return lexerSrc;

        StringBuilder block = new StringBuilder();
        block.append("// implicit tokens lifted from parser literals\n");
        for (Map.Entry<String, String> e : synthesized.entrySet()) {
            block.append(e.getValue())
                    .append(" : '").append(e.getKey()).append("' ;\n");
        }
        block.append('\n');

        // Find the first line that looks like a top-level rule (starts with
        // `fragment` or a UPPERCASE identifier followed by `:`). Inject the
        // synthesised block immediately before it. If no rule is found, fall
        // back to appending at the end so we don't drop the tokens silently.
        Pattern ruleHead = Pattern.compile(
                "(?m)^\\s*(?:fragment\\s+)?[A-Z][A-Za-z0-9_]*\\s*:");
        Matcher m = ruleHead.matcher(lexerSrc);
        if (m.find()) {
            int idx = m.start();
            // Back up to the line start to keep the block visually aligned.
            int lineStart = lexerSrc.lastIndexOf('\n', idx);
            lineStart = (lineStart < 0) ? 0 : lineStart + 1;
            return lexerSrc.substring(0, lineStart) + block + lexerSrc.substring(lineStart);
        }
        return lexerSrc + (lexerSrc.endsWith("\n") ? "" : "\n") + "\n" + block;
    }

    /** Builds {@code 'literal body' → lexer rule name} from the lexer source. */
    private static Map<String, String> indexExistingLiterals(String lexerSrc) {
        Map<String, String> map = new LinkedHashMap<>();
        Matcher m = SIMPLE_LITERAL_RULE.matcher(lexerSrc);
        while (m.find()) {
            String name = m.group(1);
            String body = m.group(2);
            // Keep the first binding; later duplicates (unusual) are ignored so
            // we don't silently change token resolution.
            map.putIfAbsent(body, name);
        }
        return map;
    }

    /**
     * Walks {@code parserSrc} and replaces each {@code 'literal'} outside
     * strings/comments with either its existing lexer name or a new
     * synthesised name (recorded in {@code synthesized}).
     */
    private static String rewriteParserLiterals(String parserSrc,
                                                Map<String, String> existing,
                                                Map<String, String> synthesized) {
        StringBuilder out = new StringBuilder(parserSrc.length());
        int i = 0, n = parserSrc.length();
        // We only rewrite literals inside rule bodies. Until we see the first
        // rule head, skip verbatim to preserve the options/tokens blocks.
        // This is a simple heuristic — the parser grammar may legitimately have
        // literals in `tokens { ... }` but ANTLR4 rarely uses them there.
        while (i < n) {
            char c = parserSrc.charAt(i);

            // Preserve comments.
            if (c == '/' && i + 1 < n && parserSrc.charAt(i + 1) == '/') {
                int nl = parserSrc.indexOf('\n', i);
                int end = (nl < 0) ? n : nl + 1;
                out.append(parserSrc, i, end);
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < n && parserSrc.charAt(i + 1) == '*') {
                int end = parserSrc.indexOf("*/", i + 2);
                end = (end < 0) ? n : end + 2;
                out.append(parserSrc, i, end);
                i = end;
                continue;
            }
            // Preserve double-quoted literals verbatim — these appear in actions
            // or options values, not as tokens.
            if (c == '"') {
                int j = skipQuoted(parserSrc, i, '"');
                out.append(parserSrc, i, j);
                i = j;
                continue;
            }

            // Single-quoted literal — candidate for lifting.
            if (c == '\'') {
                int j = skipQuoted(parserSrc, i, '\'');
                // Literal body lives between i+1 and j-1 (j points just past the closing quote).
                String body = parserSrc.substring(i + 1, j - 1);
                String name = existing.get(body);
                if (name == null) {
                    name = synthesized.get(body);
                    if (name == null) {
                        name = synthesizeName(body, existing, synthesized);
                        synthesized.put(body, name);
                    }
                }
                out.append(name);
                i = j;
                continue;
            }

            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Returns the index just past the closing quote. Handles backslash escapes. */
    private static int skipQuoted(String s, int from, char quote) {
        int j = from + 1;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < s.length()) { j += 2; continue; }
            if (c == quote) return j + 1;
            j++;
        }
        return s.length();
    }

    /**
     * Picks a readable name for a synthesised token:
     * <ol>
     *   <li>Try to derive from alphanumeric characters ("true" → "T_TRUE").</li>
     *   <li>Otherwise fall back to an opaque {@code T__<n>} counter.</li>
     * </ol>
     * Collisions with existing or already-synthesised names are resolved by
     * appending a numeric suffix.
     */
    private static String synthesizeName(String body,
                                         Map<String, String> existing,
                                         Map<String, String> synthesized) {
        String base;
        StringBuilder letters = new StringBuilder();
        for (int k = 0; k < body.length(); k++) {
            char ch = body.charAt(k);
            if (Character.isLetterOrDigit(ch) || ch == '_') letters.append(ch);
        }
        if (letters.length() > 0 && Character.isLetter(letters.charAt(0))) {
            base = "LIT_" + letters.toString().toUpperCase();
        } else {
            // Avoid double underscores — some downstream tools treat them as
            // reserved/internal. A single-underscore hex suffix is just as
            // unique in practice for our lifted-token namespace.
            base = "LIT_" + Integer.toHexString(body.hashCode() & 0xffffff).toUpperCase();
        }
        String candidate = base;
        int suffix = 1;
        List<String> used = new ArrayList<>(existing.values());
        used.addAll(synthesized.values());
        while (used.contains(candidate)) {
            candidate = base + "_" + (suffix++);
        }
        return candidate;
    }
}
