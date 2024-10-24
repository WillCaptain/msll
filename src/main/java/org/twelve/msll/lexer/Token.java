package org.twelve.msll.lexer;

import org.twelve.msll.grammarsymbol.Terminal;
import org.twelve.msll.util.Constants;

import java.io.Serializable;

/**
 * a token recognized in source code via lexer
 * in a token：
 * tokenType: is corresponding terminal, like: ID, IF, WHILE....
 * lexeme: the real word in the token, which is a word of source code
 * line: the line number of the lexeme in source code
 * start: the start column of the lexeme in source code
 * end: the end column of the lexeme in the source code
 *
 * @authorhuizi 2024
 */
public class Token implements Serializable {
    private static final long serialVersionUID = 4293840596528932667L;

    private final Terminal terminal;

    private final String lexeme;

    private final Location location;
    private String channel = "";

    public Token(Terminal terminal, String lexeme, Location location) {
        this.terminal = terminal;
        this.lexeme = lexeme;
        this.location = location;
    }

    public Token(Terminal terminal) {
        this(terminal, terminal.pattern(), new Location(-1, -1, new Line(-1, -1)));
    }

    public Terminal terminal() {
        return this.terminal;
    }

    /**
     * get the actual lexeme of the token
     *
     * @return lexeme
     */
    public String lexeme() {
        return this.lexeme.trim();
    }


    /**
     * get location of the token in source code
     *
     * @return location in source code
     */
    public Location location() {
        return this.location;
    }

    @Override
    public String toString() {
        if (this.lexeme().equals(Constants.EMPTY)) {
            return this.terminal.name();
        } else {
            return this.lexeme();
        }
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String channel(){
        return this.channel;
    }
}
