package org.twelve.msll.util;

import org.twelve.msll.lexer.Token;
import org.twelve.msll.parser.MsllStack;
import org.twelve.msll.parsetree.Flag;
import org.twelve.msll.parsetree.ParseNode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GrammarAmbiguity {
    private static AtomicInteger counter = new AtomicInteger();
    private final int id;
    private final ParseNode checkNode;
    private final Map<Token, Entry> checkEnds = new HashMap();
    private final Flag flag;
    private boolean isReady = false;

    public GrammarAmbiguity(MsllStack stack) {
        this.id = counter.incrementAndGet();
        this.checkNode = stack.getLast();
        this.flag = stack.flag();
    }

    public void makeItDone(Token token, MsllStack stack){
        if(!this.isReady) return;
        Entry prev = checkEnds.get(token);
        if(prev==null || prev.flag.expired()){
            // No previous arrival, or the previously-recorded sibling already
            // died: take ownership as the new live "first" so a later live
            // arriver isn't wrongly killed by a dead sibling's flag.
            checkEnds.put(token, new Entry(stack.flag(), stack.tokensConsumed(), stack));
            return;
        }
        // Longest-match resolution: when two live stacks both pass the same
        // checkpoint at the same token, prefer the one that has consumed more
        // input tokens. On a tie, the first arriver (grammar-order priority)
        // wins. Loser is silently expired; downstream end-of-input ambiguity
        // check skips expired stacks.
        int prevConsumed = prev.tokensConsumed;
        int currConsumed = stack.tokensConsumed();
        if (currConsumed != prevConsumed) {
            // Strictly different lengths: longest match wins.
            if (currConsumed > prevConsumed) {
                prev.stack.expire();
                checkEnds.put(token, new Entry(stack.flag(), currConsumed, stack));
            } else {
                stack.expire();
            }
            return;
        }
        // Tie on length: keep the first arriver (grammar-order priority via
        // matchNonTerminalToken's iteration of productions in grammar order).
        stack.expire();
    }

    private static final class Entry {
        final Flag flag;
        final int tokensConsumed;
        final MsllStack stack;
        Entry(Flag flag, int tokensConsumed, MsllStack stack) {
            this.flag = flag;
            this.tokensConsumed = tokensConsumed;
            this.stack = stack;
        }
    }

    public void getReady(){
        this.isReady = true;
    }

    public ParseNode checkNode() {
        return this.checkNode;
    }

    public int id(){
        return this.id;
    }
    public Flag flag(){
        return this.flag;
    }
}
