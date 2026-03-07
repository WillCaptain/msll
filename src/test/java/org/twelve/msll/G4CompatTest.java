package org.twelve.msll;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.grammar.Grammars;
import org.twelve.msll.grammar.LexerRuleCompiler;
import org.twelve.msll.grammarsymbol.NonTerminals;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parser.LexerRuleParser;
import org.twelve.msll.parserbuilder.LexerRuleParserBuilder;
import org.twelve.msll.parsetree.LexerRuleTree;
import org.twelve.msll.parsetree.NonTerminalNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the new G4-compatibility features:
 *   - fragment rules parsed and compiled to Java regex
 *   - quantifiers (+, *, ?) in lexer rule bodies
 *   - negated char classes ~[...]
 *   - char-class sequences (multi-symbol alternatives)
 *   - -> channel() and -> skip commands
 *   - mode declarations recognised (no parse error)
 */
public class G4CompatTest {

    private static LexerRuleTree parseGrammar(String resource) throws Exception {
        InputStream is = G4CompatTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(is, "Test resource not found: " + resource);
        LexerRuleParserBuilder builder = new LexerRuleParserBuilder();
        LexerRuleParser parser = builder.createParser(new InputStreamReader(is));
        return (LexerRuleTree) parser.parse();
    }

    @Test
    @SneakyThrows
    void test_fragment_rules_are_parsed() {
        LexerRuleTree tree = parseGrammar("g4CompatLexer.gm");
        List<NonTerminalNode> fragments = tree.allFragments();
        assertFalse(fragments.isEmpty(), "Expected at least one fragment rule");
        // DoubleStringChar is the fragment defined in g4CompatLexer.gm
        boolean hasDoubleStringChar = fragments.stream()
                .anyMatch(f -> f.nodes().stream()
                        .anyMatch(n -> n.toString().trim().equals("DoubleStringChar")));
        assertTrue(hasDoubleStringChar, "Expected fragment DoubleStringChar");
    }

    @Test
    @SneakyThrows
    void test_regular_grammar_rules_are_parsed() {
        LexerRuleTree tree = parseGrammar("g4CompatLexer.gm");
        List<NonTerminalNode> grammars = tree.allGrammars();
        assertFalse(grammars.isEmpty(), "Expected lexer rule nodes");
        // toString() appends a space per child, so trim before comparing
        boolean hasIdentifier = grammars.stream()
                .anyMatch(g -> g.nodes().stream()
                        .anyMatch(n -> n.toString().trim().equals("Identifier")));
        assertTrue(hasIdentifier, "Expected Identifier rule");
    }

    @Test
    @SneakyThrows
    void test_fragment_compiled_to_regex() {
        LexerRuleTree tree = parseGrammar("g4CompatLexer.gm");
        Map<String, String> frags = new LinkedHashMap<>();

        for (NonTerminalNode frag : tree.allFragments()) {
            // fragment_grammar node children (after polishing):
            //   FRAGMENT(terminal-node) + terminal(NonTerminalNode) + COLON + lex_body + SEMICOLON
            NonTerminalNode terminalNode = null;
            NonTerminalNode lexBodyNode  = null;
            for (var child : frag.nodes()) {
                if (child instanceof NonTerminalNode) {
                    String tname = child.symbol().type().name();
                    if ("terminal".equals(tname))  terminalNode = (NonTerminalNode) child;
                    if ("lex_body".equals(tname))  lexBodyNode  = (NonTerminalNode) child;
                }
            }
            if (terminalNode != null && lexBodyNode != null) {
                String name  = terminalNode.nodes().get(0).toString().trim();
                String regex = LexerRuleCompiler.compile(lexBodyNode, frags);
                frags.put(name, regex);
            }
        }

        assertTrue(frags.containsKey("DoubleStringChar"),
                "fragment DoubleStringChar should be compiled");
        String fragRegex = frags.get("DoubleStringChar");
        assertNotNull(fragRegex);
        assertFalse(fragRegex.isBlank(), "fragment regex should not be empty");
    }

    @Test
    @SneakyThrows
    void test_channels_still_parsed() {
        LexerRuleTree tree = parseGrammar("g4CompatLexer.gm");
        List<String> channels = tree.channels();
        assertEquals(2, channels.size());
        assertEquals("HIDDEN", channels.get(0));
        assertEquals("ERROR",  channels.get(1));
    }

    @Test
    @SneakyThrows
    void test_outline_lexer_still_works() {
        // Regression: the original outlineLexer.gm (no fragments) must still parse correctly.
        LexerRuleTree tree = parseGrammar("outlineLexer.gm");
        assertFalse(tree.allGrammars().isEmpty());
        assertTrue(tree.allFragments().isEmpty(), "outlineLexer.gm has no fragments");
        List<String> channels = tree.channels();
        assertEquals("HIDDEN", channels.get(0));
        assertEquals("ERROR",  channels.get(1));
    }
}
