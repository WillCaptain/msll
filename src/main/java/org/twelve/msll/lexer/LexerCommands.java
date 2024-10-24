package org.twelve.msll.lexer;

import org.twelve.msll.util.CommandCall;

import java.util.HashMap;
import java.util.Map;

/**
 * This class manages lexer commands used in G4 lexer definitions.
 * Lexer commands are responsible for controlling and modifying how tokens are processed.
 * For instance, the "channel" command can be used to assign a specific channel to a token,
 * which can help categorize tokens for further parsing stages.
 *
 * Current Implementation:
 * - The "channel" command allows assigning tokens to a specific channel for additional processing.
 *
 * Commands are managed through a centralized registry, allowing for easy extension with new commands.
 * Developers can add more lexer commands by calling `addCommand(String name, LexerCommand command)`.
 *
 * The `execute` method will execute a command if it is found in the registry. If the command is not found,
 * it will log a message indicating the command does not exist.
 * @author huizi 2024
 */
public class LexerCommands {
    private static Map<String,LexerCommand> commands = new HashMap<>();

    static{
        // Adding the "channel" command to the command registry
        addCommand("channel", (args, token) -> {
            String channel = args.get(0);
            token.setChannel(channel);
        });
    }

    /**
     * Executes a lexer command on a token.
     * If the command exists in the registry, it will be executed using the provided arguments.
     * If the command does not exist, a message will be logged.
     *
     * @param call  The command call containing the command name and arguments.
     * @param token The token to apply the command on.
     */
    public synchronized static void execute(CommandCall call, Token token) {
        LexerCommand command = commands.get(call.name());
        if(command!=null){
            command.execute(call.args(),token);
        }else{
            System.out.println("lexer command: "+call.name()+" does not exist.");
        }
    }

    /**
     * Registers a new lexer command.
     *
     * @param name    The name of the command.
     * @param command The lexer command to be registered.
     */
    public static void addCommand(String name, LexerCommand command){
        commands.put(name,command);
    }
}
