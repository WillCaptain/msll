package org.twelve.msll.grammar;

import org.twelve.msll.parsetree.NonTerminalNode;
import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.parsetree.TerminalNode;
import org.twelve.msll.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.twelve.msll.util.Tool.cast;

/**
 * Compiles a G4 lexer rule body (lex_body parse tree node) into a Java regular expression string.
 *
 * After the LexerRuleParser polishes the parse tree, the structure of a lex_body is:
 *   lex_body   → [ lex_alt, lex_alt, ... ]          (one per "|" alternative)
 *   lex_alt    → [ elem, elem, ... ]                 (sequence; elem = lex_elem or a bare atom)
 *   lex_elem   → [ atom, quantifier? ]               (only present when a quantifier is attached)
 *   atom types → STRING | SINGLE_CHARACTER | ANY | SPECIAL | REGEX | PREDICATE
 *              | terminal (NonTerminalNode = fragment reference)
 *              | lex_group (NonTerminalNode = parenthesised sub-body)
 *              | lex_not  (NonTerminalNode = NOT atom)
 *
 * Fragment names are resolved via the supplied map (name → compiled Java regex).
 *
 * @author huizi 2025
 */
public class LexerRuleCompiler {

    /**
     * Entry point: compiles a lex_body node to a Java regex string.
     *
     * @param lexBody   the lex_body NonTerminalNode from the polished parse tree
     * @param fragments map of already-compiled fragment regexes (name → java-regex)
     * @return a Java regex string equivalent to the G4 rule body
     */
    public static String compile(NonTerminalNode lexBody, Map<String, String> fragments) {
        List<String> alts = new ArrayList<>();
        for (ParseNode child : lexBody.nodes()) {
            if (child instanceof NonTerminalNode) {
                NonTerminalNode nt = cast(child);
                if (nt.name().equals(Constants.LEX_ALT)) {
                    alts.add(compileAlt(nt, fragments));
                }
            }
        }
        if (alts.isEmpty()) return "";
        if (alts.size() == 1) return alts.get(0);
        return "(?:" + String.join("|", alts) + ")";
    }

    /**
     * Compiles one alternative (lex_alt) – a sequence of elements.
     */
    private static String compileAlt(NonTerminalNode lexAlt, Map<String, String> fragments) {
        StringBuilder sb = new StringBuilder();
        for (ParseNode child : lexAlt.nodes()) {
            sb.append(compileNode(child, fragments));
        }
        return sb.toString();
    }

    /**
     * Dispatches compilation based on node type (terminal atom or non-terminal compound node).
     */
    private static String compileNode(ParseNode node, Map<String, String> fragments) {
        String typeName = node.symbol().type().name();

        if (node instanceof TerminalNode) {
            return compileTerminalAtom(cast(node));
        }

        NonTerminalNode nt = cast(node);
        switch (typeName) {
            case Constants.LEX_ELEM:
                return compileElem(nt, fragments);
            case Constants.LEX_GROUP:
                return compileGroup(nt, fragments);
            case Constants.LEX_NOT:
                return compileNot(nt, fragments);
            case Constants.TERMINAL:
                return compileFragmentRef(nt, fragments);
            default:
                // Unexpected node in an alt sequence – ignore
                return "";
        }
    }

    /**
     * Compiles a lex_elem (atom + optional quantifier).
     * Supports greedy (*, +, ?) and lazy (*?, +?) forms.
     *
     * Children after polishing:
     *   1 child  → bare atom (no quantifier; lex_elem itself was folded away, so this case
     *              should not normally reach here, but guarded for safety)
     *   2 children → atom + quantifier  (STAR | PLUS | QUESTION)
     *   3 children → atom + STAR/PLUS + QUESTION  (lazy: *? or +?)
     */
    private static String compileElem(NonTerminalNode lexElem, Map<String, String> fragments) {
        List<ParseNode> children = lexElem.nodes();
        if (children.isEmpty()) return "";

        String atomRegex = compileNode(children.get(0), fragments);

        if (children.size() >= 2) {
            String quantifier = children.get(1).toString();  // *, +, or ?
            // Lazy modifier: *? or +? (third child is another ?)
            String lazy = (children.size() == 3) ? children.get(2).toString() : "";
            atomRegex = wrapIfNeeded(atomRegex) + quantifier + lazy;
        }
        return atomRegex;
    }

    /**
     * Compiles a lex_group: ( lex_body ) → (?:...)
     */
    private static String compileGroup(NonTerminalNode lexGroup, Map<String, String> fragments) {
        // Children after polishing: LEFT_PAREN, lex_body, RIGHT_PAREN
        for (ParseNode child : lexGroup.nodes()) {
            if (child instanceof NonTerminalNode
                    && child.symbol().type().name().equals(Constants.LEX_BODY)) {
                return "(?:" + compile(cast(child), fragments) + ")";
            }
        }
        return "(?:)";
    }

    /**
     * Compiles a lex_not: ~atom
     *   ~[abc]        → [^abc]
     *   ~[^abc]       → [abc]  (double negation cancels)
     *   ~'x'          → [^x]
     *   ~FragRef       → (?:(?!frag_regex)[\s\S])
     */
    private static String compileNot(NonTerminalNode lexNot, Map<String, String> fragments) {
        // Find the atom child (skip the NOT terminal node)
        for (ParseNode child : lexNot.nodes()) {
            String childType = child.symbol().type().name();
            if (childType.equals(Constants.NOT_STR)) continue;

            if (child instanceof TerminalNode) {
                TerminalNode t = cast(child);
                if (t.name().equals(Constants.SINGLE_CHARACTER)) {
                    return negateCharClass(t.toString());
                }
                if (t.name().equals(Constants.STRING)) {
                    String s = t.toString();
                    if (s.length() == 1) return "[^" + Pattern.quote(s) + "]";
                    return "(?:(?!" + Pattern.quote(s) + ")[\\s\\S])";
                }
                // ANY, SPECIAL, etc.
                String inner = compileTerminalAtom(t);
                return "(?:(?!" + inner + ")[\\s\\S])";
            } else {
                // NonTerminalNode: lex_group or terminal (fragment ref)
                String inner = compileNode(child, fragments);
                return "(?:(?!" + inner + ")[\\s\\S])";
            }
        }
        return "[\\s\\S]";
    }

    /**
     * Compiles a fragment reference (terminal NonTerminalNode holding an UPPER_ID).
     * Looks up the fragment regex from the map; falls back to Pattern.quote of the name.
     */
    private static String compileFragmentRef(NonTerminalNode terminal, Map<String, String> fragments) {
        String name = terminal.nodes().isEmpty() ? terminal.toString()
                : terminal.nodes().get(0).toString();
        String fragRegex = fragments.get(name);
        if (fragRegex != null) {
            return "(?:" + fragRegex + ")";
        }
        // Not a fragment – could be a keyword that appears in a rule body as literal text.
        // Treat it as a quoted literal.
        return Pattern.quote(name);
    }

    /**
     * Compiles a terminal atom node based on its G4 terminal type.
     */
    private static String compileTerminalAtom(TerminalNode node) {
        String tname = node.name();
        String lexeme = node.toString();
        switch (tname) {
            case Constants.STRING:
                // Quotes already stripped by G4Parser.processTerminal
                return Pattern.quote(lexeme);
            case Constants.SINGLE_CHARACTER:
                // G4 char class → normalise to Java regex char class
                return normalizeCharClass(lexeme);
            case Constants.REGEX:
                // /"regex"/ wrapper already stripped; use as raw Java regex
                return lexeme;
            case Constants.ANY:
                // G4 '.' matches any char including newline
                return "[\\s\\S]";
            case Constants.SPECIAL:
                // \d, \s, \w, \b, etc. – directly valid in Java regex
                return lexeme;
            case Constants.PREDICATE:
                // Action/predicate code – not executable, skip
                return "";
            default:
                return Pattern.quote(lexeme);
        }
    }

    /**
     * Negates a G4 character class string.
     *   [abc]  → [^abc]
     *   [^abc] → [abc]   (remove existing negation)
     */
    private static String negateCharClass(String charClass) {
        if (charClass.startsWith("[^")) {
            return "[" + charClass.substring(2);
        }
        if (charClass.startsWith("[")) {
            return "[^" + charClass.substring(1);
        }
        return "[^" + charClass + "]";
    }

    /**
     * Converts G4 character class syntax to Java regex character class syntax.
     * Most constructs are directly compatible. Key normalisation:
     *   unicode escapes (backslash-uXXXX) – Java regex understands them natively.
     *   \p{...}  – Java regex supports Unicode properties.
     *   ~[...]   – handled upstream in compileNot().
     */
    private static String normalizeCharClass(String charClass) {
        return charClass; // currently pass-through; compatible for standard G4 char classes
    }

    /**
     * Wraps a regex in a non-capturing group if it contains alternatives or is complex,
     * so that a following quantifier applies to the whole atom rather than just its last char.
     */
    private static String wrapIfNeeded(String regex) {
        if (regex.isEmpty()) return regex;
        // Already a complete group
        if (regex.startsWith("(?:") && regex.endsWith(")") && isBalanced(regex, 2)) return regex;
        // Single char class
        if (regex.startsWith("[") && regex.endsWith("]")) return regex;
        // Single quoted literal that is one logical unit
        if (regex.startsWith("\\Q") && regex.endsWith("\\E")) return regex;
        // Single character (literal escape like \d, \s)
        if (regex.length() == 2 && regex.charAt(0) == '\\') return regex;
        // Single raw character
        if (regex.length() == 1) return regex;
        // Complex: wrap
        return "(?:" + regex + ")";
    }

    /**
     * Checks whether the non-capturing group starting at offset 'start' in 'regex'
     * is balanced (i.e., the closing ')' at the end actually closes the opening '(').
     */
    private static boolean isBalanced(String regex, int start) {
        int depth = 0;
        for (int i = start; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '(' && (i == 0 || regex.charAt(i - 1) != '\\')) depth++;
            else if (c == ')' && (i == 0 || regex.charAt(i - 1) != '\\')) depth--;
        }
        return depth == 0;
    }
}
