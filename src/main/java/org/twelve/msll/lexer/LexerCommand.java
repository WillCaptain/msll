package org.twelve.msll.lexer;

import java.util.List;

/**
 * This interface represents G4 lexer commands such as channel declarations or other custom lexer directives.
 * Lexer commands are used to modify or control the behavior of lexer rules, for example, assigning a token
 * to a specific channel. This interface provides a method to execute such commands.
 *
 * The `execute` method will take a list of built-in arguments and a token to be processed. Future extensions
 * of this interface can accommodate additional G4 lexer commands, including but not limited to:
 *  - Defining token channels (`channel` keyword).
 *  - Specifying skip or more lexer actions.
 *  - Any other lexer-specific instructions that enhance or manipulate token processing.
 *
 * Implementations of this interface should define the specific behavior for each command type.
 *
 * @author huizi 2024
 **/
interface LexerCommand {

    /**
     * Executes a lexer command using the provided built-in arguments and token.
     *
     * @param builtInArgs A list of arguments associated with the lexer command.
     * @param token The token that the lexer command will act upon.
     */
    void execute(List<String> builtInArgs, Token token);
}
