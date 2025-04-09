package org.twelve.msll.util;

import org.twelve.msll.exception.GrammarSyntaxException;
import org.twelve.msll.exception.LexerException;
import org.twelve.msll.parser.MsllStack;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for common tools and utility methods.
 *
 * The `Tool` class provides various utility methods such as generating unique IDs, logging warnings, handling grammar errors, creating instances using Unsafe,
 * and more. This class acts as a toolbox containing methods that can be used across the application for general purposes.
 *
 * Example functionalities include warning messages, error handling, generating UUIDs, reflection-based instance creation, etc.
 *
 * @author: huizi 2024
 */
public class Tool {
    // AtomicInteger for generating unique IDs
    private static final AtomicInteger id = new AtomicInteger(); // remove static

    /**
     * Logs a warning message.
     *
     * @param info The warning message to be logged.
     */
    public static void warn(String info) {
        // Currently, the method is not implemented. Could be used for logging to console or a logging system.
    }

    /**
     * Generates a new unique ID.
     *
     * @return A unique ID as a long value.
     */
    public static long newId() {
        return id.incrementAndGet();
    }

    /**
     * Throws a grammar syntax error with a given error message.
     *
     * This method will first log a warning message using {@link #warn(String)}, and then throw a {@link GrammarSyntaxException}.
     *
     * @param info The error message to be thrown.
     */
    public static void grammarError(String info) {
        warn(info);
        throw new GrammarSyntaxException(info);
    }
    /**
     * Throws a grammar syntax error with a given error message and source stack.
     *
     * This method will first log a warning message using {@link #warn(String)}, and then throw a {@link GrammarSyntaxException} with the provided stack and message.
     *
     * @param source The source stack associated with the grammar error.
     * @param info   The error message to be thrown.
     */
    public static void grammarError(MsllStack source, String info) {
        warn(info);
        throw new GrammarSyntaxException(source, info);
    }

    public static void lexerError(String info){
        warn(info);
        throw new LexerException(info);
    }

    /**
     * Generates a random UUID.
     *
     * @return A randomly generated UUID as a string.
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a new instance of the specified class using reflection and `Unsafe`, without calling the constructor.
     *
     * This method allows for the creation of an object instance without invoking its constructor. It uses the `Unsafe` class
     * to directly allocate memory for the instance.
     *
     * @param clazz The class type for which an instance is to be created.
     * @param <T>   The type of the instance.
     * @return The newly created instance of the specified type.
     * @throws Exception If an exception occurs during instance creation.
     */
    public static <T> T createInstance(Class<T> clazz) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = cast(unsafeField.get(null));
        return (T) unsafe.allocateInstance(clazz);
    }

    /**
     * Casts the provided object to the desired type.
     *
     * This method uses unchecked type casting to cast an object to the specified type.
     *
     * @param obj The object to be cast.
     * @param <T> The target type.
     * @return The object cast to the target type.
     */
    public static <T> T cast(Object obj) {
        // noinspection unchecked
        return (T) obj;
    }

    /**
     * Retrieves the file path for a grammar file by its filename.
     *
     * This method attempts to find the specified file using the class loader's resource lookup.
     *
     * @param fileName The name of the file.
     * @return The file path of the grammar file, or null if not found.
     */
    public static String getGrammarFilePath(String fileName) {
        return Optional.ofNullable(Tool.class.getClassLoader().getResource(fileName))
                .map(URL::getFile)
                .orElse(null);
    }

}

