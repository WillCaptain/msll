package org.twelve.msll.parser;

import lombok.Setter;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parsetree.Flag;
import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.util.GrammarAmbiguity;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents a stack used in the Multi-Stack LL (MSLL) parsing approach.
 * <p>
 * The stack holds `ParseNode` objects, and each stack instance maintains a `StackFlag` to indicate whether the
 * stack is expired (due to mismatches during multi-stack parsing) or still active. The stack flag also attaches
 * to related nodes in the parse tree, allowing for dynamic removal of expired nodes when the stack is expired.
 * <p>
 * Expired stacks are freed and their associated parse nodes are removed from the parse tree. This mechanism supports
 * the MSLL parser's ability to manage multiple parsing paths and handle parsing errors effectively.
 * <p>
 * huizi 2024
 */
public class MsllStack extends Stack<ParseNode> {
    /**
     * Pool of freed (unoccupied) stacks available for reuse.
     * Using an ArrayDeque gives O(1) offer/poll vs the previous O(N) linear scan
     * over the old `all` list.  The pool is cleared by reset() after each parse.
     */
    // ConcurrentLinkedDeque is fully thread-safe for poll()/offer()/clear(), which
    // prevents ConcurrentModificationException when two parse() calls run simultaneously
    // on different threads (e.g. editor typecheck racing with an LLM tool call).
    private static final ConcurrentLinkedDeque<MsllStack> freePool = new ConcurrentLinkedDeque<>();

    /**
     * Atomic counter to assign a unique index to each new stack
     */
    private static AtomicInteger counter = new AtomicInteger(0);
    /**
     * Unique index identifying this particular stack
     */
    private final int id;
    private final String grammarName;
    /**
     * Indicates if the stack is currently occupied (in use)
     */
    private boolean occupied = true;
    /**
     * The flag indicating the status (active/expired) of this stack and its related nodes
     */
    private Flag flag;

    private Map<ParseNode, GrammarAmbiguity> batches = new HashMap<>();
    private Map<ParseNode, GrammarAmbiguity> parentBatches = new HashMap<>();

    /**
     * Number of input tokens this stack has consumed. Used by the longest-match
     * priority resolver in {@link GrammarAmbiguity}: when two live stacks reach
     * the same checkpoint, the one that has consumed more tokens wins; on a
     * tie, grammar-order wins (i.e. the first arriver). Reset together with
     * the stack when it is freed back to the pool.
     */
    private int tokensConsumed = 0;

    public int tokensConsumed() { return tokensConsumed; }

    public void incrementTokensConsumed() { this.tokensConsumed++; }

    /**
     * Applies and returns an available stack, optionally copying a parent stack if provided.
     * <p>
     * This method either reuses an available (unoccupied) stack or creates a new one if none are available.
     * If a parent stack is provided, the current stack is populated with the parent stack's nodes and inherits
     * the parent's `StackFlag`.
     *
     * @param parent The parent stack to duplicate (can be null).
     * @return The applied `MsllStack`.
     */
    public static MsllStack apply(MsllStack parent, GrammarAmbiguity grammarAmbiguity, String grammarName) {
        MsllStack s = freePool.poll();  // O(1) reuse
        if (s != null) {
            s.addBatch(grammarAmbiguity);
        } else {
            s = new MsllStack(grammarAmbiguity, grammarName);
        }
        s.occupied = true;

        if (parent != null) {
            s.flag = new Flag(parent.flag);
            s.addAll(parent);
            s.parentBatches = parent.batches();
            // Forked stack inherits the parent's consumed-token count so the
            // longest-match resolver compares both stacks fairly.
            s.tokensConsumed = parent.tokensConsumed;
        } else {
            s.flag = new Flag(null);
            if (!s.parentBatches.isEmpty()) s.parentBatches = new HashMap<>();
            s.tokensConsumed = 0;
        }
        return s;
    }

    private void addBatch(GrammarAmbiguity grammarAmbiguity) {
        if (grammarAmbiguity == null) return;
        this.batches.put(grammarAmbiguity.checkNode(), grammarAmbiguity);
    }

    /**
     * Private constructor that assigns a unique index to the new stack.
     */
    private MsllStack(GrammarAmbiguity grammarAmbiguity, String grammarName) {
        this.id = counter.incrementAndGet();
        this.grammarName = grammarName;
        this.addBatch(grammarAmbiguity);
    }


    public static MsllStack apply() {
        return apply(null, null, "");
    }

    /**
     * Resets the stack system by draining the free pool.
     * Called after each completed parse to prevent stale stacks from leaking
     * across parse invocations.
     */
    public static void reset() {
        freePool.clear();
    }

    /**
     * Frees the stack by clearing its contents and returning it to the free pool for reuse.
     * The {@code occupied} guard prevents a double-add when expire() or free() is called more
     * than once on the same stack instance (a scenario that can arise from the panic-mode
     * catch block in MsllParser.parseToken).
     */
    public void free() {
        if (!this.occupied) return;  // guard: already freed, do not re-add to pool
        this.occupied = false;
        this.clear();
        this.batches.clear();
        this.tokensConsumed = 0;
        freePool.offer(this);
    }

    /**
     * Marks the stack as expired and frees it.
     * <p>
     * When a stack is marked as expired (due to mismatches during parsing), its associated
     * `StackFlag` is also marked as expired. The stack is then freed and its contents are cleared.
     */
    public void expire() {
        this.flag.expire();
        this.free();
    }


    /**
     * Pushes a `ParseNode` onto the stack and attaches the stack's flag to the node.
     * <p>
     * Each `ParseNode` added to the stack is tagged with the stack's flag, allowing the parser to
     * track and manage the node's status (active or expired) during the parsing process.
     *
     * @param node The `ParseNode` to push onto the stack.
     * @return The pushed `ParseNode`.
     */
    @Override
    public ParseNode push(ParseNode node) {
        node.setFlag(this.flag);
        return super.push(node);
    }

    /**
     * Generates a hash code for the stack, including its unique index and contents for map match
     */
    @Override
    public synchronized int hashCode() {
        return Objects.hash(this.id, this);
    }

    /**
     * Compares two stacks by their unique index to determine equality for map match
     *
     * @param stack The object to compare with.
     * @return True if the stacks have the same index, false otherwise.
     */
    @Override
    public synchronized boolean equals(Object stack) {
        if (stack instanceof MsllStack) {
            return ((MsllStack) stack).id == this.id;
        } else {
            return false;
        }
    }

    public ParseNode pop(Token token) {
        ParseNode popped = super.pop();
        GrammarAmbiguity grammarAmbiguity = null;
        if (!this.isEmpty()) {
            // Hot path: avoid creating a merged HashMap on every pop.
            // Two direct O(1) lookups replace the previous O(N) map-merge.
            grammarAmbiguity = this.batches.get(popped);
            if (grammarAmbiguity == null) grammarAmbiguity = this.parentBatches.get(popped);
        }
        // Only use a non-hidden token as the disambiguation key.
        // Hidden-channel tokens (e.g. SingleLineComment) must never be passed to
        // makeItDone() because they are skipped by the parser loop and therefore
        // arrive as the "previous token" for the first real token in a file.
        // Using a hidden token as the key causes both ambiguous stacks to be
        // incorrectly marked as ambiguous, leaving dead parse-tree nodes behind.
        if (grammarAmbiguity != null && token.channel().isEmpty()) {
            grammarAmbiguity.makeItDone(token, this);
        }
        return popped;
    }

    private Map<ParseNode, GrammarAmbiguity> batches() {
        Map<ParseNode, GrammarAmbiguity> bs = new HashMap<>();
        bs.putAll(this.batches);
        bs.putAll(this.parentBatches);
        return bs;
    }

    public void setAmbiguous(Flag flag) {
        this.flag.expire();
        this.free();
    }

//    public void setAmbiguous(Object flag) {
//        this.setAmbiguous(flag, true);
//    }

    public Flag flag() {
        return this.flag;
    }
}


