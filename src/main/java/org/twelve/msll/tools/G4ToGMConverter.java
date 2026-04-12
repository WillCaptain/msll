package org.twelve.msll.tools;

import java.io.*;
import java.nio.file.*;
import java.util.regex.*;

/**
 * Converts ANTLR4 .g4 grammar files to MSLL .gm format
 *
 * Key conversions:
 * 1. Remove EOF from parser rules
 * 2. Rename first parser rule to 'root' if needed
 * 3. Convert character classes [a-z] to regex format /"[a-z]"/
 * 4. Keep other syntax mostly the same
 */
public class G4ToGMConverter {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java G4ToGMConverter <input.g4> [output.gm]");
            System.out.println("If output not specified, will use input name with .gm extension");
            return;
        }

        String inputPath = args[0];
        String outputPath = args.length > 1 ? args[1] :
            inputPath.replaceAll("\\.g4$", ".gm");

        String content = Files.readString(Path.of(inputPath));
        String converted = convert(content, inputPath.contains("Lexer"));

        Files.writeString(Path.of(outputPath), converted);
        System.out.println("Converted " + inputPath + " -> " + outputPath);
    }

    public static String convert(String content, boolean isLexer) {
        String result = content;

        if (isLexer) {
            result = convertLexer(result);
        } else {
            result = convertParser(result);
        }

        return result;
    }

    private static String convertLexer(String content) {
        String result = content;

        // Convert character classes to regex format if not already
        // [a-z] -> /"[a-z]"/
        // But be careful not to convert if already in quotes or regex format
        result = convertCharacterClasses(result);

        return result;
    }

    private static String convertParser(String content) {
        String result = content;

        // Remove EOF from rules
        // Example: "program : stmt* EOF ;" -> "program : stmt* ;"
        result = result.replaceAll("\\s+EOF\\s*;", " ;");
        result = result.replaceAll("\\s+EOF\\s*\\|", " |");

        // Find first parser rule and rename to 'root' if not already
        Pattern firstRulePattern = Pattern.compile(
            "^([a-z][a-zA-Z0-9_]*)\\s*:",
            Pattern.MULTILINE
        );
        Matcher matcher = firstRulePattern.matcher(result);
        if (matcher.find()) {
            String firstRuleName = matcher.group(1);
            if (!firstRuleName.equals("root")) {
                // Rename first rule to root
                result = result.replaceFirst(
                    "^" + firstRuleName + "\\s*:",
                    "root:"
                );
                System.out.println("  Renamed first rule '" + firstRuleName + "' to 'root'");
            }
        }

        return result;
    }

    private static String convertCharacterClasses(String content) {
        // This is a simplified conversion
        // In a full implementation, would need to parse more carefully
        // to avoid converting things inside strings, comments, etc.

        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");

        for (String line : lines) {
            // Skip comments and already converted lines
            if (line.trim().startsWith("//") ||
                line.trim().startsWith("/*") ||
                line.contains("/\"")) {
                result.append(line).append("\n");
                continue;
            }

            // Look for patterns like: RULE : [a-z]+ ;
            // Convert to: RULE : /"[a-z]+"/ ;
            String converted = line;

            // Pattern: : [character class]+ or * or ?
            Pattern pattern = Pattern.compile(
                ":\\s*([\\[\\]a-zA-Z0-9_\\-~\\\\]+[+*?]?)\\s*;"
            );
            Matcher matcher = pattern.matcher(line);
            if (matcher.find() && line.contains("[")) {
                String charClass = matcher.group(1);
                if (!charClass.startsWith("\"") && !charClass.startsWith("/")) {
                    converted = line.replace(
                        ": " + charClass + " ;",
                        ": /\"" + charClass + "\"/ ;"
                    );
                    converted = converted.replace(
                        ":" + charClass + ";",
                        ": /\"" + charClass + "\"/ ;"
                    );
                }
            }

            result.append(converted).append("\n");
        }

        return result.toString();
    }

    /**
     * Converts a standard ANTLR4 grammar to MSLL format
     */
    public static String convertGrammar(String g4Content, boolean isLexer) {
        return convert(g4Content, isLexer);
    }
}
