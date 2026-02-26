package org.twelve.msll.exception;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when the MSLL parser encounters multiple syntax errors during
 * error-recovery mode. Each individual error is preserved so that tools
 * and IDEs can report all problems in a single compilation pass instead
 * of stopping at the first failure.
 *
 * @author huizi 2025
 */
public class AggregateGrammarSyntaxException extends GrammarSyntaxException {

    private final List<GrammarSyntaxException> errors;

    public AggregateGrammarSyntaxException(List<GrammarSyntaxException> errors) {
        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    /** All individual parse errors collected during error-recovery parsing. */
    public List<GrammarSyntaxException> errors() {
        return errors;
    }

    private static String buildMessage(List<GrammarSyntaxException> errors) {
        return errors.stream()
                .map(Throwable::getMessage)
                .collect(Collectors.joining("\n─────────────────────────────────\n",
                        "Found " + errors.size() + " syntax error(s):\n",
                        ""));
    }
}
