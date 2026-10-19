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
public class MsllStack {

    /**
     * Backing storage. Historically this class extended {@link java.util.Stack}
     * (hence {@link java.util.Vector}), which made every push/pop/peek in the
     * parser's innermost loop take a monitor lock. Stacks are confined to one
     * parse invocation, so an unsynchronized ArrayList is safe and much faster.
     */
    private final ArrayList<ParseNode> nodes = new ArrayList<>(32);
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

    /**
     * Ambiguity checkpoints visible to this stack, including ancestors.
     * Published roots are immutable, so a fork shares the parent root and
     * {@link AmbiguityIndex#with} copies only the path to the new checkpoint.
     * {@link #free()} drops this stack's reference and does not mutate a root
     * a descendant still holds.
     */
    private AmbiguityIndex index = AmbiguityIndex.EMPTY;

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
        if (s == null) {
            s = new MsllStack(grammarName);
        }
        s.occupied = true;
        AmbiguityIndex base = parent == null ? AmbiguityIndex.EMPTY : parent.index;
        s.index = grammarAmbiguity == null ? base : base.with(grammarAmbiguity.checkNode(), grammarAmbiguity);

        if (parent != null) {
            s.flag = new Flag(parent.flag);
            // Direct element copy (no setFlag): forked nodes keep their original flags,
            // matching the historical Vector.addAll behaviour.
            s.nodes.addAll(parent.nodes);
            // Forked stack inherits the parent's consumed-token count so the
            // longest-match resolver compares both stacks fairly.
            s.tokensConsumed = parent.tokensConsumed;
        } else {
            s.flag = new Flag(null);
            s.tokensConsumed = 0;
        }
        return s;
    }

    /**
     * Private constructor that assigns a unique index to the new stack.
     */
    private MsllStack(String grammarName) {
        this.id = counter.incrementAndGet();
        this.grammarName = grammarName;
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
        this.nodes.clear();
        // Drop the reference. The previous root stays intact for any descendant
        // that captured it before this stack was freed.
        this.index = AmbiguityIndex.EMPTY;
        this.tokensConsumed = 0;
        freePool.offer(this);
    }

    // ── Stack surface (previously inherited from java.util.Stack) ───────────

    public ParseNode pop() {
        if (nodes.isEmpty()) throw new EmptyStackException();
        return nodes.remove(nodes.size() - 1);
    }

    public ParseNode peek() {
        if (nodes.isEmpty()) throw new EmptyStackException();
        return nodes.get(nodes.size() - 1);
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
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
    public ParseNode push(ParseNode node) {
        node.setFlag(this.flag);
        nodes.add(node);
        return node;
    }

    /**
     * Hash by unique index, consistent with {@link #equals(Object)}. (The old
     * contents-based {@code Objects.hash(id, this)} would have recursed into
     * itself — proof it was never actually called.)
     */
    @Override
    public int hashCode() {
        return this.id;
    }

    /**
     * Compares two stacks by their unique index to determine equality for map match
     *
     * @param stack The object to compare with.
     * @return True if the stacks have the same index, false otherwise.
     */
    @Override
    public boolean equals(Object stack) {
        if (stack instanceof MsllStack) {
            return ((MsllStack) stack).id == this.id;
        } else {
            return false;
        }
    }

    public ParseNode pop(Token token) {
        ParseNode popped = this.pop();
        GrammarAmbiguity grammarAmbiguity = null;
        if (!this.isEmpty()) {
            grammarAmbiguity = this.index.get(popped);
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


