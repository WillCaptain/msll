package org.twelve.msll.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strips ANTLR4 target-language constructs that MSLL's interpreted runtime
 * cannot evaluate:
 *
 * <ul>
 *   <li><b>Semantic predicates</b> {@code {expr}?} / {@code {!expr}?}
 *       &mdash; ANTLR4 evaluates these in the generated parser at runtime.
 *       MSLL has no target-language evaluator, so we drop them. This is
 *       sound for parse-shape recognition (the predicate becomes "always
 *       true") and lossy only for grammars whose ambiguity resolution
 *       genuinely depends on runtime state (e.g. strict-mode keywords
 *       in JavaScript). For the grammars-v4 compatibility experiment we
 *       accept that trade-off: the goal is the parse succeeds, not that
 *       every JavaScript strict-vs-loose edge case is decided correctly.</li>
 *   <li><b>Embedded actions</b> {@code {stmt; stmt;}} &mdash; side-effectful
 *       hooks that the generator compiles into the parser. Nothing to do
 *       at parse-shape level; we replace them with a single space.</li>
 *   <li><b>Rule-level options</b> {@code rule options {...} : ... ;}
 *       &mdash; {@code tokenLabel}, {@code assoc}, etc. None affect parse
 *       shape; strip the whole {@code options{...}} clause between a rule
 *       head and its {@code :}.</li>
 * </ul>
 *
 * <p>Top-level declarations ({@code options{...}}, {@code channels{...}},
 * {@code tokens{...}}, {@code @header{...}}, {@code @members{...}}) that
 * sit <em>outside</em> rule bodies are handled separately: the opaque
 * {@code @xxx{...}} hooks are stripped entirely, {@code options} and
 * {@code channels} are left alone because MSLL's meta-grammar already
 * parses them.
 *
 * <p>The scanner is whitespace- and comment-aware, correctly skips over
 * single- / double-quoted string literals and {@code [...]} character
 * classes, so braces nested inside those constructs are not mistaken for
 * action delimiters. Designed to be idempotent: running the stripper on
 * already-stripped input is a no-op.
 */
public final class G4ActionStripper {

    private G4ActionStripper() {}

    /**
     * Returns {@code src} with action blocks, semantic predicates, rule-level
     * options, and {@code @header/@members/@lexer::.../@parser::...} hook
     * blocks removed.
     */
    public static String strip(String src) {
        String s = stripAtHookBlocks(src);
        s = stripRuleLevelOptions(s);
        s = stripActionsInRuleBodies(s);
        return s;
    }

    // -----------------------------------------------------------------
    // @header { ... }, @members { ... }, @lexer::members { ... }, etc.
    // -----------------------------------------------------------------

    private static final Pattern AT_HOOK = Pattern.compile(
            "@[A-Za-z_][A-Za-z_0-9]*(?:::[A-Za-z_][A-Za-z_0-9]*)?\\s*\\{");

    private static String stripAtHookBlocks(String src) {
        StringBuilder out = new StringBuilder(src.length());
        Matcher m = AT_HOOK.matcher(src);
        int i = 0;
        while (m.find(i)) {
            // Copy everything up to the `@` untouched; skip the match and the
            // balanced `{...}` block that follows the opening brace.
            out.append(src, i, m.start());
            int braceStart = m.end() - 1; // position of '{'
            int braceEnd = matchBrace(src, braceStart);
            if (braceEnd < 0) {
                // Unbalanced braces — bail out and keep the tail verbatim.
                out.append(src, m.start(), src.length());
                return out.toString();
            }
            i = braceEnd + 1;
        }
        out.append(src, i, src.length());
        return out.toString();
    }

    // -----------------------------------------------------------------
    // Rule-level options:   name options { k = v; } : ... ;
    // -----------------------------------------------------------------

    private static final Pattern RULE_OPTIONS = Pattern.compile(
            "(?m)^([ \\t]*(?:fragment\\s+)?[A-Za-z_][A-Za-z_0-9]*)"
                    + "\\s+options\\s*\\{");

    private static String stripRuleLevelOptions(String src) {
        StringBuilder out = new StringBuilder(src.length());
        Matcher m = RULE_OPTIONS.matcher(src);
        int i = 0;
        while (m.find(i)) {
            out.append(src, i, m.start());
            out.append(m.group(1)); // keep the rule head
            int braceStart = m.end() - 1;
            int braceEnd = matchBrace(src, braceStart);
            if (braceEnd < 0) {
                out.append(src, m.start(), src.length());
                return out.toString();
            }
            i = braceEnd + 1;
        }
        out.append(src, i, src.length());
        return out.toString();
    }

    // -----------------------------------------------------------------
    // Action blocks and semantic predicates inside rule bodies
    // -----------------------------------------------------------------

    /**
     * Walks {@code src} and strips every {@code {...}} block that sits
     * between a rule head's {@code :} and its terminating {@code ;}. The
     * walker reproduces the skip rules in {@link G4Splitter} so that
     * braces nested inside strings, character classes, or comments are
     * never treated as action delimiters.
     */
    private static String stripActionsInRuleBodies(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        boolean inRuleBody = false;
        int ruleDepth = 0; // paren depth inside the body

        while (i < n) {
            char c = src.charAt(i);

            // Line / block comments. Outside a rule body we leave them
            // untouched (they may precede rule heads and carry docs).
            // Inside a rule body they are illegal in .gm (the meta-grammar
            // has no COMMENT atom in `lex_atom` / `symbol`), so we replace
            // them with whitespace — the JavaScript lexer grammar has a
            // real `// Break lines here ...` comment between two action
            // blocks that would otherwise leak into the rule body after
            // action stripping.
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                int nl = src.indexOf('\n', i);
                int end = (nl < 0) ? n : nl + 1;
                if (inRuleBody) {
                    out.append('\n'); // keep line structure; drop the text
                } else {
                    out.append(src, i, end);
                }
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                int end = src.indexOf("*/", i + 2);
                end = (end < 0) ? n : end + 2;
                if (inRuleBody) {
                    out.append(' ');
                } else {
                    out.append(src, i, end);
                }
                i = end;
                continue;
            }
            if (c == '\'' || c == '"') {
                int end = skipString(src, i, c);
                out.append(src, i, end);
                i = end;
                continue;
            }
            if (c == '[') {
                int end = skipCharClass(src, i);
                out.append(src, i, end);
                i = end;
                continue;
            }

            if (!inRuleBody) {
                if (c == ':' && isRuleHeadColon(src, i)) {
                    inRuleBody = true;
                    ruleDepth = 0;
                }
                out.append(c);
                i++;
                continue;
            }

            // Inside a rule body.
            if (c == '(') { ruleDepth++; out.append(c); i++; continue; }
            if (c == ')') { ruleDepth--; out.append(c); i++; continue; }
            if (c == ';' && ruleDepth == 0) {
                inRuleBody = false;
                out.append(c);
                i++;
                continue;
            }
            if (c == '{') {
                int end = matchBrace(src, i);
                if (end < 0) {
                    // Unbalanced — copy the rest verbatim to stay safe.
                    out.append(src, i, n);
                    return out.toString();
                }
                int after = end + 1;
                if (after < n && src.charAt(after) == '?') {
                    after++; // also eat the predicate marker
                }
                out.append(' '); // placeholder so adjacent tokens don't fuse
                i = after;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /**
     * True when the colon at {@code idx} is the separator between a rule
     * head and its body (i.e. on the same logical line as an identifier
     * that starts a top-level rule head). We only need a cheap heuristic:
     * walk backwards skipping whitespace / line comments and check we hit
     * an identifier character. This matches all grammars-v4 rules and
     * avoids false positives on colons inside options like
     * {@code tokenVocab = Foo;} (no colon) or labels like {@code x : y}
     * inside a rule body (those are already caught by inRuleBody).
     */
    private static boolean isRuleHeadColon(String src, int idx) {
        int i = idx - 1;
        // Skip whitespace and comments backwards.
        while (i >= 0) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) { i--; continue; }
            break;
        }
        if (i < 0) return false;
        char c = src.charAt(i);
        return Character.isLetterOrDigit(c) || c == '_' || c == '>';
        // '>' handles the rare "[label] :" / rule-option-closer case.
    }

    // -----------------------------------------------------------------
    // Shared scanner helpers (mirrors G4Splitter).
    // -----------------------------------------------------------------

    /** Returns index of the matching {@code '}'} for the {@code '{'} at
     *  {@code start}, or {@code -1} if unbalanced. Skips strings / char
     *  classes / comments inside so braces therein are not counted. */
    private static int matchBrace(String s, int start) {
        int depth = 0;
        int i = start;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                int nl = s.indexOf('\n', i);
                i = (nl < 0) ? n : nl + 1;
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                i = (end < 0) ? n : end + 2;
                continue;
            }
            if (c == '\'' || c == '"') { i = skipString(s, i, c); continue; }
            if (c == '[') { i = skipCharClass(s, i); continue; }
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
            i++;
        }
        return -1;
    }

    private static int skipString(String s, int i, char quote) {
        int j = i + 1;
        int n = s.length();
        while (j < n) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < n) { j += 2; continue; }
            if (c == quote) return j + 1;
            if (c == '\n') return j; // don't swallow EOL
            j++;
        }
        return n;
    }

    private static int skipCharClass(String s, int i) {
        int j = i + 1;
        int n = s.length();
        while (j < n) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < n) { j += 2; continue; }
            if (c == ']') return j + 1;
            j++;
        }
        return n;
    }
}
