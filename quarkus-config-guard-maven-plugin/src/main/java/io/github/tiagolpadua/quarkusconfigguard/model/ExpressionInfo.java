package io.github.tiagolpadua.quarkusconfigguard.model;

/**
 * Represents information extracted from an expression language reference ${...}.
 */
public class ExpressionInfo {

    /** The full expression string, e.g. "${OTEL_EXPORTER_OTLP_ENABLED:false}" */
    private final String fullExpression;

    /** The variable name inside the expression, e.g. "OTEL_EXPORTER_OTLP_ENABLED" */
    private final String variableName;

    /** The default value if present, e.g. "false" */
    private final String defaultValue;

    /** Whether the variable looks like an environment variable (ALL_CAPS_WITH_UNDERSCORES) */
    private final boolean environmentVariable;

    /** Whether the variable looks like a property reference (contains lowercase letters or dots) */
    private final boolean propertyReference;

    public ExpressionInfo(String fullExpression, String variableName, String defaultValue) {
        this.fullExpression = fullExpression;
        this.variableName = variableName;
        this.defaultValue = defaultValue;
        this.environmentVariable = variableName != null && variableName.matches("[A-Z][A-Z0-9_]*");
        this.propertyReference = variableName != null && (variableName.contains(".") || variableName.matches(".*[a-z].*"));
    }

    public String getFullExpression() {
        return fullExpression;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isEnvironmentVariable() {
        return environmentVariable;
    }

    public boolean isPropertyReference() {
        return propertyReference;
    }

    @Override
    public String toString() {
        return fullExpression;
    }
}
