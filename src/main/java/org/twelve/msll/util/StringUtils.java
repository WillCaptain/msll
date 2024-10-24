package org.twelve.msll.util;

import java.util.List;
import java.util.Objects;

/**
 * Utility class for String operations.
 *
 * The `StringUtils` class contains a collection of static methods that provide various utilities for working with strings.
 * This class is designed to be a utility class, and hence is declared as `final` to prevent inheritance.
 * It also contains a private constructor to prevent instantiation.
 *
 * Example utilities might include methods to check if a string is empty, check if it contains only whitespace,
 * or any other common operations that involve manipulating or evaluating strings.
 *
 * Usage:
 * All methods are declared as static, so the utility functions can be called directly using the class name,
 * e.g., `StringUtils.isBlank(String str)`.
 *
 * Note:
 * Since this is a utility class, it follows the singleton pattern of being non-instantiable and final.
 *
 * @author huizi 2024
 */
public final class StringUtils {

    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }
    /**
     * Checks if the given string is {@code null}, empty, or contains only whitespace characters.
     *
     * This method is useful for determining whether a string is considered "blank,"
     * meaning that it has no characters or only contains whitespace characters (spaces, tabs, etc.).
     *
     * @param source The {@link String} to check.
     * @return {@code true} if the string is {@code null}, empty, or contains only whitespace characters;
     *         otherwise, {@code false}.
     */
    public static boolean isBlank(String source) {
        if (isEmpty(source)) {
            return true;
        }
        for (int i = 0; i < source.length(); i++) {
            if (!Character.isWhitespace(source.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given string is {@code null} or an empty string.
     *
     * This method is useful for determining whether a string is "empty," meaning that it has no characters
     * or is {@code null}. This is often used to validate if the string has content or not.
     *
     * @param source The {@link String} to check.
     * @return {@code true} if the string is {@code null} or empty (i.e., length is 0); {@code false} otherwise.
     */
    public static boolean isEmpty(String source) {
        return source == null || source.isEmpty();
    }


    /**
     * Converts a list of elements to a string representation, concatenating each element with a space in between.
     *
     * This method iterates over the provided list and appends the string representation of each element,
     * followed by a space character, to a {@link StringBuilder}. It then returns the final concatenated string.
     *
     * This method is useful when you need a single string containing all elements of a list, separated by spaces.
     *
     * @param lst The {@link List} containing elements to be converted to a string.
     *            It can be a list of any type of objects since each element will be converted to its string representation.
     * @return A {@link String} containing all elements of the list, separated by spaces.
     *         If the list is empty, the resulting string will be empty as well.
     */
    public static String parse(List lst) {
        StringBuilder sb = new StringBuilder();
        for (Object i : lst) {
            sb.append(i.toString()+" ");
        }
        return sb.toString();
    }

    /**
     * Escapes special characters in a string to make it suitable for use in a regular expression.
     *
     * In regular expressions, certain characters have special meanings (e.g., brackets, parentheses, etc.).
     * This method ensures that those characters are escaped properly, so that they are treated literally
     * and do not affect the behavior of the regex.
     *
     * @param str The input string that potentially contains regex special characters.
     * @return A string where all regex special characters have been escaped, making the string suitable for literal matching.
     *
     * Special characters that are escaped include:
     * '[', ']', '{', '}', '(', ')', '*', '+', '?', '^', '$', '\', '|', '/', and '.'.
     */
    public static String escapeRegex(String str) {
        return str.replaceAll("([\\[\\]{}()*+?^$\\\\|/\\.])", "\\\\$1");//.replace("\\","\\\\");
    }
}
