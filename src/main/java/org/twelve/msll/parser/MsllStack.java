package org.twelve.msll.parser;

import lombok.Setter;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.parsetree.Flag;
import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.util.GrammarAmbiguity;

import java.util.*;
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
     * A list to keep track of all active and available MSLL stacks
     */
    private static List<MsllStack> all = new ArrayList<>();
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
        Optional<MsllStack> stack = all.stream().filter(s -> !s.occupied).findFirst();
        MsllStack s;
        if (stack.isPresent()) {
            s = stack.get();
            s.addBatch(grammarAmbiguity);
        } else {
            s = new MsllStack(grammarAmbiguity, grammarName);
        }
        s.occupied = true;

        if (parent != null) {
            s.flag = new Flag(parent.flag);
            s.addAll(parent);
            s.parentBatches = parent.batches();
        } else {
            s.flag = new Flag(null);
        }
        return s;
    }

    private void addBatch(GrammarAmbiguity grammarAmbiguity) {
        if (grammarAmbiguity == null) return;
        this.batches.put(grammarAmbiguity.checkNode(), grammarAmbiguity);
    }

    /**
     * Private constructor that assigns a unique index to the stack and adds it to the list of all stacks.
     */
    private MsllStack(GrammarAmbiguity grammarAmbiguity, String grammarName) {
        this.id = counter.incrementAndGet();
        this.grammarName = grammarName;
        all.add(this);
        this.addBatch(grammarAmbiguity);
    }


    public static MsllStack apply() {
        return apply(null, null, "");
    }

    /**
     * Resets the stack system by clearing all existing stacks.
     * <p>
     * This method clears all active and available stacks, typically used to reset the parsing system
     * before starting a new parsing operation.
     */
    public static void reset() {
        all.clear();
    }

    /**
     * Frees the stack by marking it as unoccupied and clearing its contents.
     * <p>
     * This method is called when a stack is no longer needed in the parsing process, making it
     * available for reuse in future parsing operations.
     */
    public void free() {
        this.occupied = false;
        this.clear();
        this.batches.clear();
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
            grammarAmbiguity = this.batches().get(popped);
        }
        if (grammarAmbiguity != null) {
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
        this.flag.setAmbiguous(flag);
//        Flag parent = this.flag;
//        while (parent != flag) {
//            parent.setAmbiguous();
//            parent = parent.parent();
//        }
//        if (isFree) {
            this.free();
//        }
    }

//    public void setAmbiguous(Object flag) {
//        this.setAmbiguous(flag, true);
//    }

    public Flag flag() {
        return this.flag;
    }
}


