package org.twelve.msll.parsetree;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The MSLL stack flag associated with parse nodes.
 * This flag tracks whether a stack has failed during the parsing process.
 *
 * If a stack match fails, the associated flag is marked as expired. Nodes
 * with this flag will then be removed during the parse tree's polish phase,
 * ensuring that only valid paths remain in the final parse tree.
 *
 * @return true if the flag is expired (i.e., the stack match failed), false otherwise.
 *
 * @author huizi 2024
 */
public class Flag {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int id;
    private boolean expired = false;
    private final Flag parent;
    private final List<Flag> children = new ArrayList<>();
    private boolean ambiguous = false;

    /**
     * Constructor that links a parent flag to this flag.
     *
     * @param parent The parent `StackFlag` (can be null).
     */
    public Flag(Flag parent) {
        this.parent = parent;
        if (parent != null) {
            parent.children.add(this);
        }
        this.id = counter.getAndIncrement();
    }

    /**
     * Checks if this flag is expired.
     *
     * @return True if the flag is expired, false otherwise.
     */
    public boolean expired() {
        return this.expired;
    }

    public Boolean isAmbiguous(){
        return this.ambiguous;
    }
    private void setAmbiguous() {
        this.ambiguous = true;
    }

    public void setAmbiguous(Flag aligned){
        Flag parent = this;
        while (parent != aligned) {
            parent.setAmbiguous();
            parent = parent.parent();
        }
    }

    public void expire() {
        this.expired = true;
        if (this.parent != null) {
            this.parent.children.remove(this);
            if (this.parent.children.isEmpty()) {
                this.parent.expire();
            }
        }
    }

    public Flag parent() {
        return this.parent;
    }
}
