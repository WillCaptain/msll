package org.twelve.msll;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.exception.GrammarSyntaxException;
import org.twelve.msll.parser.MsllParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the stray-backslash NPE.
 *
 * <p>A character the grammar has no terminal for (e.g. a bare {@code \}) must
 * surface as a well-formed {@link GrammarSyntaxException} that names the
 * offending character/position, never a raw {@link NullPointerException}.
 *
 * <p>Historically the lexer swallowed the {@code LexerException} and appended a
 * {@code null} sentinel token to the buffer; {@code MsllParser.parse()} then
 * dereferenced that null while probing the buffer's trailing END token, throwing
 * an NPE that got wrapped as {@code "parsing error: Cannot invoke Token.terminal()
 * ... is null"}. This test locks in the clean-error behaviour.
 */
public class StrayBackslashTest {

    private MsllParser<?> parserFor(String code) throws Exception {
        MyParserBuilder builder = new MyParserBuilder("outlineParser.gm", "outlineLexer.gm");
        return builder.createParser(code);
    }

    @Test
    @SneakyThrows
    void stray_backslash_is_clean_syntax_error_not_npe() {
        // Literal backslash-quote characters in the token stream, the exact
        // shape that triggered the NPE during BIRD eval.
        String code = "clients.filter(c -> c.gender == \\\"F\\\").count()";

        GrammarSyntaxException ex = assertThrows(GrammarSyntaxException.class,
                () -> parserFor(code).parse());

        // The message must name the offending character and its position, and
        // must NOT be the wrapped NPE we used to emit.
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        assertTrue(msg.contains("Unexpected character"),
                () -> "expected a positioned lexer error, got: " + msg);
        assertTrue(msg.contains("\\"),
                () -> "expected the message to name the stray backslash, got: " + msg);
        assertTrue(!msg.contains("Cannot invoke") && !msg.contains("Token.terminal()"),
                () -> "must not leak the old NullPointerException, got: " + msg);
    }

    @Test
    @SneakyThrows
    void lone_backslash_is_clean_syntax_error_not_npe() {
        // A single stray backslash anywhere the grammar has no terminal for it.
        GrammarSyntaxException ex = assertThrows(GrammarSyntaxException.class,
                () -> parserFor("let a = \\;").parse());
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        assertTrue(msg.contains("Unexpected character"),
                () -> "expected a positioned lexer error, got: " + msg);
    }

    @Test
    @SneakyThrows
    void well_formed_input_still_parses() {
        // Guard against over-eager rejection: ordinary well-formed input (including
        // properly-quoted string literals) must still parse cleanly.
        assertDoesNotThrow(() -> parserFor("let a = \"F\";").parse());
    }
}
