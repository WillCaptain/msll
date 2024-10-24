package org.twelve.msll.parser;

import javafx.util.Pair;
import org.twelve.msll.lexer.Token;
import org.twelve.msll.lexer.TokenBuffer;
import org.twelve.msll.util.Constants;
import org.twelve.msll.util.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class interprets and processes G4 predicate conditions that are typically enclosed within curly braces, such as `{notLineTerminator}`.
 *
 * Grammar predicates are used to apply additional constraints or conditions to grammar rules, ensuring that certain conditions
 * (like "not being a line terminator") are met before the parsing of a token or sequence of tokens is allowed to continue.
 *
 * The class stores and interprets predicates as functions that evaluate based on the token input and additional arguments.
 * A predefined set of predicate functions are registered (such as `notLineTerminator`), but the class is designed to be extendable
 * by allowing additional functions to be added via `addFunction`.
 *
 * huizi 2024
 */
public class GrammarPredicate {
    /**
     * A map storing registered predicate functions.
     * Each function is a BiPredicate that takes a pair of token input and token buffer, along with additional arguments
     */
    private static Map<String, BiPredicate<Pair<Token, List<Token>>, List>> functions = new HashMap<>();

    /**
     *  Static initializer that adds built-in predicate functions
     */
    static {
        //Adds the "notLineTerminator" predicate, which checks if the current token is not an end-of-line (EOL) token
        addFunction("notLineTerminator",(args1,args2)-> !args1.getKey().terminal().name().equals(Constants.EOL_STR));
        //// Adds a generic "n" predicate, which compares the current token's lexeme to a specific string passed as an argument
        addFunction("n",(args1,args2)-> {
            Token token = args1.getKey();
            String key = args2.get(0).toString();
            return token.lexeme().trim().equals(key.trim());
        });
    }
    private final List args = new ArrayList();
    private String funcName;

    /**
     * Parses the provided predicate code and extracts the function name and its arguments.
     *
     * The input code is expected to follow the pattern of a function call with optional arguments,
     * such as `notLineTerminator()` or `n("someArg")`. The function name is stored in `funcName`,
     * and the arguments are parsed and stored in the `args` list.
     *
     * @param code The string representing the predicate function.
     */
    public GrammarPredicate(String code) {
        String FUNC = "func", ARGS = "args";
        Pattern pattern = Pattern.compile("(\\bthis\\.)?(?<func>\\w+)(\\((?<args>.*)\\))?");
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            String func = matcher.group(FUNC);
            String args = matcher.group(ARGS);
            if (func != null) {
                this.funcName = func;
            }
            if (args != null && !args.trim().equals(Constants.EMPTY)) {
                for (String arg : args.split(",")) {
                    if (arg.startsWith("\"")) {
                        this.args.add(arg.replace("\"", ""));
                    } else {
                        if (arg.contains(".")) this.args.add(Double.parseDouble(arg));
                        else this.args.add(Long.parseLong(arg));
                    }
                }
            }
        }
    }

    /**
     * Executes the predicate function by applying it to the given token and token buffer.
     *
     * The `test` method retrieves the function corresponding to `funcName` and applies it to the current token and token buffer.
     * If the predicate returns false, a grammar error is raised.
     *
     * @param token The current token being evaluated by the predicate.
     * @param tokens The buffer of tokens (context for the predicate function).
     */
    public void test(Token token, TokenBuffer tokens) {
        BiPredicate<Pair<Token, List<Token>>, List> function = functions.get(this.funcName);
        if (function == null) return;//forget it, function is not found, then ignore the predicate
        if (!function.test(new Pair(token, tokens), this.args)) {
            Tool.grammarError("grammar predicate fail:"+this.funcName);
        }

    }

    /**
     * Adds a new predicate function to the registry.
     *
     * This method allows external code to register new predicate functions. Each function
     * is defined as a `BiPredicate`, which takes a pair (token, token buffer) and a list of arguments.
     *
     * @param name The name of the predicate function.
     * @param predicate The function logic for the predicate.
     */
    public static void addFunction(String name, BiPredicate<Pair<Token, List<Token>>, List> predicate) {
        functions.put(name,predicate);
    }

    /**
     * Returns the name of the predicate function.
     *
     * @return The name of the predicate function.
     */
    public String funcName() {
        return this.funcName;
    }

    /**
     * Returns the list of arguments for the predicate function.
     *
     * @return The list of arguments passed to the function.
     */
    public List args() {
        return this.args;
    }
}
