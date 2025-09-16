package org.twelve.msll.exception;

/**
 * Enumerates all error codes in the GCP compiler/interpreter system.
 *
 * <p>Errors are categorized by phase and severity for better handling.
 * Each code should have a corresponding user-friendly message in resources.
 */
public enum ParseErrCode {
    OPERATOR_MISMATCH, // --- Syntax/Structure Errors ---
    TYPE_DECLARED_ERROR
}