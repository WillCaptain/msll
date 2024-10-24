package org.twelve.msll.lexer;

import lombok.SneakyThrows;
import org.twelve.msll.exception.LexerException;
import org.twelve.msll.grammarsymbol.Terminals;
import org.twelve.msll.util.CommandCall;
import org.twelve.msll.util.Constants;

import java.io.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * lexer
 * parse source code to tokens for the next parser step
 * lexer will output a token stream and asyncally emitting token to parser
 *
 * @author huizi 2024
 */
public abstract class Lexer {
    protected final Terminals terminals;
    protected final Reader reader;
    protected final CodeCache codeCache;

    public Lexer(Reader reader, Terminals terminals) {
        this.reader = reader;
        this.terminals = terminals;
        this.codeCache = new CodeCache();
    }

    private int charIndex = 0;
    private int lineIndex = 0;

    @SneakyThrows
    private int read(Reader reader) {
        return reader.read();
    }

    protected boolean isEnter(char ch) {
        return ch == '\r' || ch == '\n';
    }

    public TokenBuffer scan() {
        TokenBuffer buffer = new TokenBuffer(this.codeCache);
        CompletableFuture.runAsync(() -> {
            StringBuilder line = new StringBuilder();
            char ch = ' ';
            int _ch;
            while (true) {
                _ch = read(reader);
                if (eolOrEof(_ch)) {
                    if (eolOrEof(ch) && _ch != Constants.EOF && ch != (char) _ch) continue;
                    ch = (char) _ch;
                    lineIndex = this.codeCache.addLine(line.toString());
                    line = new StringBuilder();
                    //handle EOF
                    if (_ch == Constants.EOF) {
                        this.tokenize((char) _ch, charIndex, lineIndex, handleToken(buffer));
                        break;
                    }
                } else {
                    ch = (char) _ch;
                    line.append(ch);
                }
                this.tokenize(ch, charIndex++, lineIndex, handleToken(buffer));
            }

        }).exceptionally(t -> {
            t.printStackTrace();
            buffer.addToken(null);
            return null;
        });
        return buffer;
    }

    protected Consumer<Token> handleToken(TokenBuffer buffer) {
        return token -> {
            buffer.addToken(token);
            String command = token.terminal().getCommand();
            if (command != null) {
                String[] commands = command.substring(2).split(",");
                for (String cmd : commands) {
                    CommandCall call = CommandCall.parse(cmd);
                    LexerCommands.execute(call, token);
                }
            }
        };
    }

    private boolean eolOrEof(int _ch) {
        char ch = (char) _ch;
        return this.isEnter(ch) || _ch == Constants.EOF;
    }

    /**
     * different lexer use different way to tokenize the source code
     * like: regex way, dfa way
     * msll will use regex to tokenize
     *
     * @param ch        tokenize from a single char
     * @param charIndex location of the char
     * @param lineIndex line number of the char
     * @param consumer  next action when match a token
     */
    protected abstract void tokenize(char ch, int charIndex, int lineIndex, Consumer<Token> consumer);


}
