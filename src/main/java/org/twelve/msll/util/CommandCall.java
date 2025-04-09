package org.twelve.msll.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for parsing lexer command calls to build a {@code CommandCall} object for a given terminal.
 * The command call typically includes a function name followed by optional arguments enclosed in parentheses.
 *
 * @author huizi 2024
 */
public class CommandCall {

    /**
     * Regex pattern to parse a function call string. The pattern supports a function name followed by
     * optional arguments enclosed in parentheses.
     * - The function name must start with a letter or underscore, followed by alphanumeric characters or underscores.
     * - The arguments are optional and can be enclosed in parentheses.
     */
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:\\(([^)]*)\\))?\\s*$"
    );

    // The name of the function or command.
    private final String name;

    // The list of arguments provided for the function call.
    private final List<String> args;

    /**
     * Constructs a {@code CommandCall} with a specific name and argument list.
     *
     * @param name The name of the command or function.
     * @param args A list of arguments for the function call.
     */
    public CommandCall(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    /**
     * Parses a command call string into a {@code CommandCall} object.
     * The command should be in the format: "functionName(arg1, arg2, ...)".
     *
     * @param command The command string to be parsed.
     * @return A {@code CommandCall} object representing the parsed command.
     * @throws IllegalArgumentException if the command format is invalid.
     */
    public synchronized static CommandCall parse(String command) {
        Matcher matcher = FUNCTION_PATTERN.matcher(command);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid function call format: " + command);
        }

        String name = matcher.group(1);
        String argsString = matcher.group(2);

        List<String> args = new ArrayList<>();
        if (argsString != null && !argsString.trim().isEmpty()) {
            // Split arguments by comma and trim whitespace
            String[] splitArgs = argsString.split(",");
            for (String arg : splitArgs) {
                args.add(arg.trim());
            }
        }
        return new CommandCall(name, args);
    }

    /**
     * Gets the name of the command or function.
     *
     * @return The name of the command or function.
     */
    public String name() {
        return this.name;
    }

    /**
     * Gets the list of arguments for the command.
     *
     * @return A list of arguments.
     */
    public List<String> args() {
        return this.args;
    }
}
