package org.twelve.msll.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits an ANTLR4 <em>combined</em> grammar (<code>grammar X;</code>) into
 * the two halves MSLL expects: a lexer grammar and a parser grammar.
 *
 * <p>ANTLR4 recognises three grammar forms:
 * <ul>
 *   <li><code>lexer grammar X;</code> &mdash; lexer-only, already split.</li>
 *   <li><code>parser grammar X;</code> &mdash; parser-only, already split.</li>
 *   <li><code>grammar X;</code> &mdash; <em>combined</em>: lexer rules
 *       (UPPERCASE names + {@code fragment}s) and parser rules (lowercase names)
 *       live in the same file. This is the dominant form in the
 *       <a href="https://github.com/antlr/grammars-v4">grammars-v4</a> corpus.</li>
 * </ul>
 *
 * <p>The split is purely syntactic &mdash; no semantic analysis &mdash; which is
 * sufficient for MSLL's compatibility experiments. If the grammar is already
 * lexer-only or parser-only, the non-applicable half comes back empty.
 */
public final class G4Splitter {

    private G4Splitter() {}

    /** One grammar worth of source, already split into halves. */
    public static final class Split {
        public final Form form;
        public final String lexer;
        public final String parser;
        public Split(Form form, String lexer, String parser) {
            this.form = form;
            this.lexer = lexer;
            this.parser = parser;
        }
    }

    public enum Form { LEXER_ONLY, PARSER_ONLY, COMBINED }

    /** Matches {@code grammar X;} / {@code lexer grammar X;} / {@code parser grammar X;}. */
    private static final Pattern HEADER = Pattern.compile(
            "(?m)^\\s*(lexer|parser)?\\s*grammar\\s+([A-Za-z_][A-Za-z_0-9]*)\\s*;");

    /**
     * Matches a top-level rule head: either {@code NAME :} (lexer) or
     * {@code name :} (parser). The separator between the name and the colon
     * tolerates not only whitespace but also line comments (<code>// ...</code>)
     * and block comments (<code>/* ... *&#47;</code>) because real ANTLR4
     * grammars in {@code grammars-v4} routinely put doc comments between the
     * name and the colon (e.g. JSON.g4's {@code fragment INT}). Dropping such
     * rules silently used to strip the lexer of critical fragments.
     */
    private static final Pattern RULE_HEAD = Pattern.compile(
            "(?m)^(fragment\\s+)?([A-Za-z_][A-Za-z_0-9]*)"
                    + "(?:\\s|//[^\\n]*\\n|/\\*[\\s\\S]*?\\*/)*"
                    + ":");

    /**
     * Matches ANTLR4 lexer-mode switch declarations such as {@code mode VALUE;}
     * that may appear between lexer rules. These are not rules (no colon, no
     * body) but they partition subsequent lexer rules into named modes; the
     * splitter must preserve them so the lexer half still carries the mode
     * boundaries MSLL's {@link org.twelve.msll.parsetree.LexerRuleTree}
     * relies on. Only recognised at the start of a line to stay out of rule
     * bodies.
     */
    private static final Pattern MODE_DECL = Pattern.compile(
            "(?m)^\\s*mode\\s+[A-Za-z_][A-Za-z_0-9]*\\s*;");

    /**
     * Splits {@code src} into lexer / parser halves.
     *
     * @param src raw contents of a {@code .g4} file
     * @param baseName the grammar's symbol name (used when synthesising
     *                 headers for a combined grammar), e.g. {@code "JSON"};
     *                 may be {@code null} to let this method extract it.
     */
    public static Split split(String src, String baseName) {
        Matcher h = HEADER.matcher(src);
        if (!h.find()) {
            // No header found: treat the whole file as a parser body.
            return new Split(Form.PARSER_ONLY, "", src);
        }
        String kind = h.group(1);
        String rawName = baseName != null ? baseName : h.group(2);
        // Lexer grammars in MSLL must start with an uppercase letter
        // (matches the UPPER_ID terminal in ParserGrammar.gm).
        String name = rawName.isEmpty()
                ? "G"
                : Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1);

        if ("lexer".equals(kind)) {
            return new Split(Form.LEXER_ONLY, src, "");
        }
        if ("parser".equals(kind)) {
            return new Split(Form.PARSER_ONLY, "", src);
        }

        // Combined grammar → split rule-by-rule.
        // Strategy: walk each top-level rule declaration, then classify by its
        // head's first character (uppercase → lexer, lowercase → parser).
        // The file's prologue (comments, options, tokens, channels, etc.) is
        // copied into the lexer half; the parser half starts fresh with a
        // `parser grammar` header that references the lexer via tokenVocab.
        String body = src.substring(h.end()).stripLeading();
        String prologue = src.substring(0, h.start());

        List<int[]> ruleSpans = collectRuleSpans(body);
        StringBuilder lexerRules = new StringBuilder();
        StringBuilder parserRules = new StringBuilder();

        int headerLessPrologueLen = 0;
        // Everything before the first rule is shared prologue (options/channels/tokens/comments).
        if (!ruleSpans.isEmpty()) {
            headerLessPrologueLen = ruleSpans.get(0)[0];
        }
        String sharedPrologue = body.substring(0, headerLessPrologueLen);

        for (int i = 0; i < ruleSpans.size(); i++) {
            int[] span = ruleSpans.get(i);
            String ruleText = body.substring(span[0], span[1]);
            if (isModeDecl(ruleText) || isLexerRule(ruleText)) {
                lexerRules.append(ruleText);
                if (!ruleText.endsWith("\n")) lexerRules.append('\n');
            } else {
                parserRules.append(ruleText);
                if (!ruleText.endsWith("\n")) parserRules.append('\n');
            }
        }

        String lexer = prologue
                + "lexer grammar " + name + "Lexer;\n"
                + sharedPrologue
                + lexerRules;
        String parser = prologue
                + "parser grammar " + name + "Parser;\n"
                // MSLL's .gm grammar requires the options block to start on the
                // line after '{' (matches ParserGrammarParser's options_statement).
                + "options {\n    tokenVocab = " + name + "Lexer;\n}\n"
                + parserRules;
        return new Split(Form.COMBINED, lexer, parser);
    }

    /**
     * Locates {@code [start, end)} byte spans of every top-level rule in
     * {@code body}. A rule ends at a semicolon at brace/bracket/paren depth
     * zero; string literals and comments are skipped so delimiters inside
     * them cannot terminate the rule by mistake.
     */
    private static List<int[]> collectRuleSpans(String body) {
        List<int[]> spans = new ArrayList<>();
        Matcher m = RULE_HEAD.matcher(body);
        while (m.find()) {
            int start = m.start();
            int end = findRuleEnd(body, m.end());
            if (end < 0) break;
            spans.add(new int[]{start, end});
        }
        // Mode declarations are single-statement lines (mode X;) that sit
        // between lexer rules. They don't match RULE_HEAD (no colon/body)
        // so we collect them separately and merge in position order so the
        // final span list stays sorted.
        Matcher md = MODE_DECL.matcher(body);
        while (md.find()) {
            spans.add(new int[]{md.start(), md.end()});
        }
        spans.sort((a, b) -> Integer.compare(a[0], b[0]));
        return spans;
    }

    /**
     * True if the span's first non-whitespace characters are {@code mode }.
     * Mode declarations belong to the lexer half unconditionally.
     */
    private static boolean isModeDecl(String ruleText) {
        int i = 0, n = ruleText.length();
        while (i < n && Character.isWhitespace(ruleText.charAt(i))) i++;
        return ruleText.startsWith("mode ", i) || ruleText.startsWith("mode\t", i);
    }

    /** Scans forward from {@code from} until the terminating {@code ;} at depth 0. */
    private static int findRuleEnd(String body, int from) {
        int depth = 0;
        int i = from;
        while (i < body.length()) {
            char c = body.charAt(i);
            // Skip line comments.
            if (c == '/' && i + 1 < body.length() && body.charAt(i + 1) == '/') {
                int nl = body.indexOf('\n', i);
                i = (nl < 0) ? body.length() : nl + 1;
                continue;
            }
            // Skip block comments.
            if (c == '/' && i + 1 < body.length() && body.charAt(i + 1) == '*') {
                int end = body.indexOf("*/", i + 2);
                i = (end < 0) ? body.length() : end + 2;
                continue;
            }
            // Skip string literals (both ' and " forms).
            if (c == '\'' || c == '"') {
                i = skipString(body, i, c);
                continue;
            }
            // Skip character classes — brackets inside them are not structural.
            if (c == '[') {
                i = skipCharClass(body, i);
                continue;
            }
            if (c == '(' || c == '{') depth++;
            else if (c == ')' || c == '}') depth--;
            else if (c == ';' && depth == 0) {
                return i + 1;
            }
            i++;
        }
        return -1;
    }

    private static int skipString(String s, int i, char quote) {
        int j = i + 1;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < s.length()) { j += 2; continue; }
            if (c == quote) return j + 1;
            j++;
        }
        return s.length();
    }

    private static int skipCharClass(String s, int i) {
        int j = i + 1;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == '\\' && j + 1 < s.length()) { j += 2; continue; }
            if (c == ']') return j + 1;
            j++;
        }
        return s.length();
    }

    /**
     * A lexer rule begins with {@code fragment} or an UPPERCASE name; a parser
     * rule begins with a lowercase name. We look at the first identifier
     * character we can find in {@code ruleText}.
     */
    private static boolean isLexerRule(String ruleText) {
        int i = 0, n = ruleText.length();
        while (i < n && Character.isWhitespace(ruleText.charAt(i))) i++;
        if (ruleText.startsWith("fragment", i)) return true;
        if (i < n) {
            char c = ruleText.charAt(i);
            return Character.isUpperCase(c);
        }
        return false;
    }
}
