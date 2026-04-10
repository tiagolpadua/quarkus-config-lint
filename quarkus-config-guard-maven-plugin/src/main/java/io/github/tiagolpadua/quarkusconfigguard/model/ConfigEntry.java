package io.github.tiagolpadua.quarkusconfigguard.model;

import java.util.List;

/**
 * Represents a single entry from a Quarkus application.properties file.
 */
public class ConfigEntry {

    /** The raw key as it appeared in the file (e.g., "%dev.quarkus.otel.exporter.otlp.enabled") */
    private final String rawKey;

    /** The value as it appeared in the file */
    private final String rawValue;

    /** The parsed profile (%dev, %prod, etc.) or null for the default profile */
    private final Profile profile;

    /** The property key without the profile prefix (e.g., "quarkus.otel.exporter.otlp.enabled") */
    private final String propertyKey;

    /** Source file name (relative path) */
    private final String sourceFile;

    /** Line number in the source file (1-based) */
    private final int lineNumber;

    /** Expressions found in the value */
    private final List<ExpressionInfo> expressions;

    public ConfigEntry(
            String rawKey,
            String rawValue,
            Profile profile,
            String propertyKey,
            String sourceFile,
            int lineNumber,
            List<ExpressionInfo> expressions) {
        this.rawKey = rawKey;
        this.rawValue = rawValue;
        this.profile = profile;
        this.propertyKey = propertyKey;
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.expressions = expressions;
    }

    public String getRawKey() {
        return rawKey;
    }

    public String getRawValue() {
        return rawValue;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<ExpressionInfo> getExpressions() {
        return expressions;
    }

    public boolean hasExpressions() {
        return !expressions.isEmpty();
    }
}
