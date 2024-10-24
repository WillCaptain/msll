package org.twelve.msll.grammar;

/**
 * Represents the associativity in the G4 grammar association section.
 * Associativity defines the order in which operations of the same precedence are evaluated:
 * - NONE: No associativity is defined for this production.
 * - LEFT: Operations are evaluated from left to right (left-associative).
 * - RIGHT: Operations are evaluated from right to left (right-associative).
 *
 * @author huizi 2024
 */
public enum ASSOC {
    NONE,LEFT,RIGHT
}
