package org.twelve.msll.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.parsetree.TerminalNode;
import org.twelve.msll.util.GrammarAmbiguity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AmbiguityIndexTest {

    @BeforeEach
    void resetStacks() {
        MsllStack.reset();
    }

    @Test
    void overrideKeepsThePreviousSnapshot() {
        ParseNode key = node();
        GrammarAmbiguity first = ambiguity();
        GrammarAmbiguity second = ambiguity();
        AmbiguityIndex older = AmbiguityIndex.EMPTY.with(key, first);
        AmbiguityIndex newer = older.with(key, second);
        assertSame(first, older.get(key));
        assertSame(second, newer.get(key));
        assertNull(AmbiguityIndex.EMPTY.get(key));
    }

    @Test
    void randomForksMatchAHashMap() {
        Random random = new Random(3);
        List<ParseNode> keys = new ArrayList<>();
        for (int i = 0; i < 80; i++) keys.add(node());
        AmbiguityIndex index = AmbiguityIndex.EMPTY;
        Map<ParseNode, GrammarAmbiguity> reference = new HashMap<>();
        List<AmbiguityIndex> snaps = new ArrayList<>();
        List<Map<ParseNode, GrammarAmbiguity>> snapRefs = new ArrayList<>();
        for (int step = 0; step < 400; step++) {
            if (step % 17 == 0) {
                snaps.add(index);
                snapRefs.add(new HashMap<>(reference));
            }
            ParseNode key = keys.get(random.nextInt(keys.size()));
            GrammarAmbiguity value = ambiguity();
            index = index.with(key, value);
            reference.put(key, value);
        }
        assertMatches(index, reference);
        for (int i = 0; i < snaps.size(); i++) {
            assertMatches(snaps.get(i), snapRefs.get(i));
        }
    }

    @Test
    void identicalHashesStayDistinctUntilTheKeyMatches() {
        ParseNode firstKey = colliding();
        ParseNode secondKey = colliding();
        GrammarAmbiguity first = ambiguity();
        GrammarAmbiguity second = ambiguity();
        GrammarAmbiguity replaced = ambiguity();
        AmbiguityIndex both = AmbiguityIndex.EMPTY.with(firstKey, first).with(secondKey, second);
        AmbiguityIndex overridden = both.with(firstKey, replaced);
        assertSame(first, both.get(firstKey));
        assertSame(second, both.get(secondKey));
        assertSame(replaced, overridden.get(firstKey));
        assertSame(second, overridden.get(secondKey));
        assertSame(first, both.get(firstKey));
    }

    @Test
    void childKeepsTheCheckpointAfterTheParentIsFreed() {
        ParseNode below = node();
        ParseNode checkpoint = node();
        MsllStack parent = MsllStack.apply();
        parent.push(below);
        parent.push(checkpoint);
        GrammarAmbiguity shared = new GrammarAmbiguity(parent);
        shared.getReady();
        MsllStack left = MsllStack.apply(parent, shared, "g");
        MsllStack right = MsllStack.apply(parent, shared, "g");
        parent.free();
        parent.free();

        Token token = new Token(Terminal.EPSILON);
        assertSame(checkpoint, left.pop(token));
        assertSame(checkpoint, right.pop(token));
        assertFalse(left.flag().expired());
        assertTrue(right.flag().expired());

        MsllStack.reset();
        left.free();
        MsllStack reused = MsllStack.apply();
        assertSame(left, reused);
        reused.push(node());
        reused.push(checkpoint);
        assertSame(checkpoint, reused.pop(token));
        assertFalse(reused.flag().expired());
    }

    private static void assertMatches(AmbiguityIndex index, Map<ParseNode, GrammarAmbiguity> reference) {
        for (Map.Entry<ParseNode, GrammarAmbiguity> entry : reference.entrySet()) {
            assertSame(entry.getValue(), index.get(entry.getKey()));
        }
    }

    private static ParseNode node() {
        return new TerminalNode(new Symbol<>(Terminal.EPSILON, "k"));
    }

    private static ParseNode colliding() {
        return new TerminalNode(new Symbol<>(Terminal.EPSILON, "c")) {
            @Override
            public int hashCode() {
                return 0x12345678;
            }
        };
    }

    private static GrammarAmbiguity ambiguity() {
        MsllStack stack = MsllStack.apply();
        stack.push(node());
        GrammarAmbiguity ambiguity = new GrammarAmbiguity(stack);
        stack.free();
        return ambiguity;
    }
}
