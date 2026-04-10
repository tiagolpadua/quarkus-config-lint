package io.github.tiagolpadua.quarkusconfigguard.parser;

import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.model.ExpressionInfo;
import io.github.tiagolpadua.quarkusconfigguard.model.Profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Quarkus application.properties files into {@link ConfigEntry} objects.
 *
 * <p>Supports:
 * <ul>
 *   <li>Standard key=value lines</li>
 *   <li>Quarkus profile prefixes (%dev., %prod., %staging., etc.)</li>
 *   <li>Expression language detection (${...})</li>
 *   <li>Comments (lines starting with # or !)</li>
 *   <li>Continuation lines (ending with \)</li>
 * </ul>
 */
public class PropertiesParser {

    /** Pattern to detect profile prefix: %<name>.<rest> */
    private static final Pattern PROFILE_PATTERN = Pattern.compile("^%([^.]+)\\.(.+)$");

    /** Pattern to detect expression language: ${variable} or ${variable:default} */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    /**
     * Parses a properties file and returns all config entries found.
     *
     * @param file       the properties file to parse
     * @param sourceFile relative path to use in error messages
     * @return list of parsed config entries
     * @throws IOException if the file cannot be read
     */
    public List<ConfigEntry> parse(File file, String sourceFile) throws IOException {
        List<ConfigEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            StringBuilder continuationBuilder = null;
            int continuationStart = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Handle continuation lines
                if (continuationBuilder != null) {
                    String trimmed = line.stripLeading();
                    if (line.endsWith("\\")) {
                        continuationBuilder.append(trimmed, 0, trimmed.length() - 1);
                        continue;
                    } else {
                        continuationBuilder.append(trimmed);
                        line = continuationBuilder.toString();
                        lineNumber = continuationStart;
                        continuationBuilder = null;
                    }
                } else if (line.endsWith("\\") && !line.endsWith("\\\\")) {
                    continuationBuilder = new StringBuilder();
                    continuationStart = lineNumber;
                    String trimmed = line.strip();
                    continuationBuilder.append(trimmed, 0, trimmed.length() - 1);
                    continue;
                }

                // Skip comments and empty lines
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }

                // Parse key=value (also supports : separator)
                int separatorIdx = findSeparatorIndex(trimmed);
                if (separatorIdx < 0) {
                    // Key with no value — treat value as empty
                    String rawKey = trimmed.strip();
                    entries.add(buildEntry(rawKey, "", sourceFile, lineNumber));
                    continue;
                }

                String rawKey = trimmed.substring(0, separatorIdx).strip();
                String rawValue = trimmed.substring(separatorIdx + 1).stripLeading();

                entries.add(buildEntry(rawKey, rawValue, sourceFile, lineNumber));
            }
        }

        return entries;
    }

    /**
     * Finds the index of the key/value separator (= or :) in a line,
     * skipping escaped characters.
     */
    private int findSeparatorIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                i++; // skip escaped char
                continue;
            }
            if (c == '=' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    private ConfigEntry buildEntry(String rawKey, String rawValue, String sourceFile, int lineNumber) {
        Profile profile;
        String propertyKey;

        Matcher profileMatcher = PROFILE_PATTERN.matcher(rawKey);
        if (profileMatcher.matches()) {
            profile = new Profile(profileMatcher.group(1));
            propertyKey = profileMatcher.group(2);
        } else {
            profile = Profile.DEFAULT;
            propertyKey = rawKey;
        }

        List<ExpressionInfo> expressions = parseExpressions(rawValue);

        return new ConfigEntry(rawKey, rawValue, profile, propertyKey, sourceFile, lineNumber, expressions);
    }

    /**
     * Parses all expression language references from a value string.
     */
    List<ExpressionInfo> parseExpressions(String value) {
        List<ExpressionInfo> result = new ArrayList<>();
        Matcher matcher = EXPRESSION_PATTERN.matcher(value);

        while (matcher.find()) {
            String fullExpression = matcher.group(0);
            String inner = matcher.group(1);

            // Split on first colon for default value
            int colonIdx = inner.indexOf(':');
            String variableName;
            String defaultValue;
            if (colonIdx >= 0) {
                variableName = inner.substring(0, colonIdx);
                defaultValue = inner.substring(colonIdx + 1);
            } else {
                variableName = inner;
                defaultValue = null;
            }

            result.add(new ExpressionInfo(fullExpression, variableName, defaultValue));
        }

        return result;
    }
}
