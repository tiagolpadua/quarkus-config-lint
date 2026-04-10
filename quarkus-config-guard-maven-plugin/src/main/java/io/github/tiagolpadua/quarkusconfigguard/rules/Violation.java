package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;

/**
 * Represents a single lint violation found by a {@link Rule}.
 */
public class Violation {

    public enum Severity {
        ERROR,
        WARNING
    }

    private final String ruleId;
    private final Severity severity;
    private final String message;
    private final ConfigEntry entry;
    private final String reason;
    private final String suggestion;

    public Violation(
            String ruleId,
            Severity severity,
            String message,
            ConfigEntry entry,
            String reason,
            String suggestion) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
        this.entry = entry;
        this.reason = reason;
        this.suggestion = suggestion;
    }

    public String getRuleId() {
        return ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public ConfigEntry getEntry() {
        return entry;
    }

    public String getReason() {
        return reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public boolean isWarning() {
        return severity == Severity.WARNING;
    }
}
