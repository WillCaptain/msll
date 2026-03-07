package org.twelve.msll.grammarsymbol;

import org.twelve.msll.parser.Symbol;
import org.twelve.msll.parsetree.TerminalNode;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.RegexString;

import java.util.regex.Pattern;

import static org.twelve.msll.util.StringUtils.escapeRegex;

/**
 * Represents a terminal symbol in the grammar.
 * Terminals can either be matched by a literal string (keyword) or a regex pattern, depending on their configuration.
 * @author huizi 2024
 */
public class Terminal extends SymbolType {

    // Special terminal symbols for grammar, such as epsilon and whitespace
    public final static Terminal EPSILON = new Terminal(Constants.EPSILON_STR, Constants.EPSILON);
    public static final Terminal WHITESPACE = new Terminal(Constants.WHITESPACE_STR, Constants.WHITESPACE);
    /**
     * Indicates if the terminal is defined using a regex pattern.
     * If true, the terminal matches input using regex.
     * If false, the terminal is matched with a literal string (keywords or specific symbols).
     */
    private boolean isRegex;
    // The pattern used to match this terminal, either a regex or a complete match string.
    private String pattern;
    // An optional G4 command associated with this terminal (e.g., for lexer instructions).
    private String command = null;
    // P1: compiled Pattern cache — built once on first use, invalidated when refresh() is called.
    private volatile Pattern compiledPattern = null;

    /**
     * The lexer mode this terminal belongs to.
     * null = built-in / always active (regardless of mode).
     * "DEFAULT_MODE" = active only in the default lexer mode.
     * Any other value = active only in the named mode.
     */
    private String mode = null;

    /**
     * Constructor for a terminal with a literal match pattern.
     *
     * @param name    The name of the terminal.
     * @param pattern The match pattern, usually a literal string.
     */
    public Terminal(String name, String pattern) {
        super(name);
        this.pattern = pattern.equals(Constants.WHITESPACE) ? pattern : pattern.trim();
        this.isRegex = false;
    }

    /**
     * Constructor for a terminal with a regex match pattern.
     *
     * @param name  The name of the terminal.
     * @param regex A regex object that describes the terminal's match criteria.
     */
    public Terminal(String name, RegexString regex) {
        super(name);
        this.pattern = regex.toString().trim();
        this.isRegex = true;
    }


    /**
     * Returns the compiled {@link Pattern} for this terminal, building and caching it on first call.
     * Eliminates the O(N_terminals × M_positions) Pattern.compile() cost in the lexer hot path.
     */
    public Pattern compiledPattern() {
        if (compiledPattern == null) {
            String inner = this.isRegex ? this.pattern.trim() : escapeRegex(this.pattern);
            compiledPattern = Pattern.compile(String.format("(?<%s>%s)", this.tokenName(), inner));
        }
        return compiledPattern;
    }

    /**
     * Returns the regex string for this terminal (kept for compatibility).
     */
    public String regex() {
        String regex = this.isRegex ? this.pattern.trim() : escapeRegex(this.pattern);
        return String.format("(?<%s>%s)", this.tokenName(), regex);
    }

    /**
     * Creates a parse tree node for this terminal.
     *
     * @param symbol The symbol representing this terminal.
     * @return A TerminalNode object associated with this terminal.
     */
    @Override
    public TerminalNode parse(Symbol symbol) {
        return new TerminalNode(symbol);
    }

    /**
     * Retrieves the match pattern for this terminal.
     *
     * @return The pattern, either a regex or a literal match string.
     */
    public String pattern() {
        return this.pattern;
    }

    /**
     * Refreshes the current terminal with the properties of another terminal.
     *
     * @param terminal The terminal from which properties are copied.
     */
    public void refresh(Terminal terminal) {
        this.name = terminal.name;
        this.pattern = terminal.pattern;
        this.isRegex = terminal.isRegex;
        this.command = terminal.command;
        this.mode = terminal.mode;
        this.compiledPattern = null;  // invalidate cache on update
    }

    /**
     * Gets the token name used for regex group naming.
     * Strips out special characters (e.g., underscores) for naming purposes.
     *
     * @return The token name for this terminal.
     */
    public String tokenName() {
        String name = this.name();
        return name.replace(Constants.UNDER_LINE, "");
    }

    /**
     * Determines if this terminal is defined by a regex.
     * It will help matcher to determine the literal match priority is higher than regex match
     * @return True if the terminal is a regex, otherwise false.
     */
    public boolean isRegex() {
        return this.isRegex;
    }

    /**
     * Sets the command associated with this terminal, typically used for lexer operations.
     *
     * @param command The lexer command for this terminal.
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Gets the lexer command associated with this terminal.
     *
     * @return The lexer command.
     */
    public String getCommand() {
        return this.command;
    }

    /** Returns the lexer mode this terminal belongs to (null = always active). */
    public String mode() {
        return this.mode;
    }

    /** Assigns this terminal to a specific lexer mode. */
    public void setMode(String mode) {
        this.mode = mode;
    }
}
