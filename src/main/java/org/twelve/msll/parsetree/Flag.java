package org.twelve.msll.parsetree;

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
public interface Flag {
    /**
     * Indicates whether the stack match has failed, causing the flag to expire.
     * Expired flags signal the parser to remove the corresponding nodes from the parse tree.
     *
     * @return true if the flag is expired, false otherwise.
     */
    boolean expired();
}
