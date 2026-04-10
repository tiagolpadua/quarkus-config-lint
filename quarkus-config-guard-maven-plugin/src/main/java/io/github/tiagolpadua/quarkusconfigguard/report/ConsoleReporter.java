package io.github.tiagolpadua.quarkusconfigguard.report;

import io.github.tiagolpadua.quarkusconfigguard.rules.Violation;
import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * Outputs lint violations to the Maven build console using the Maven {@link Log}.
 */
public class ConsoleReporter {

    private final Log log;

    public ConsoleReporter(Log log) {
        this.log = log;
    }

    /**
     * Reports all violations to the console.
     *
     * @param violations the violations to report
     */
    public void report(List<Violation> violations) {
        if (violations.isEmpty()) {
            log.info("quarkus-config-guard: No violations found.");
            return;
        }

        log.info("quarkus-config-guard: Found " + violations.size() + " violation(s):");

        for (Violation violation : violations) {
            String header = String.format("[%s] %s: %s",
                    violation.getSeverity(),
                    violation.getRuleId(),
                    violation.getMessage());

            String location = String.format("        %s=%s",
                    violation.getEntry().getRawKey(),
                    violation.getEntry().getRawValue());

            String key = String.format("        key: %s", violation.getEntry().getPropertyKey());

            String file = String.format("        file: %s (line %d)",
                    violation.getEntry().getSourceFile(),
                    violation.getEntry().getLineNumber());

            String reason = "        reason: " + violation.getReason();
            String suggestion = "        suggestion: " + violation.getSuggestion();

            String fullMessage = "\n" + header + "\n" + location + "\n" + key
                    + "\n" + file + "\n" + reason + "\n" + suggestion;

            if (violation.isError()) {
                log.error(fullMessage);
            } else {
                log.warn(fullMessage);
            }
        }
    }

    /**
     * Prints a summary of violations to the console.
     *
     * @param violations the violations to summarize
     */
    public void reportSummary(List<Violation> violations) {
        long errors = violations.stream().filter(Violation::isError).count();
        long warnings = violations.stream().filter(Violation::isWarning).count();

        if (errors == 0 && warnings == 0) {
            log.info("quarkus-config-guard: Lint passed with no issues.");
        } else {
            log.info(String.format(
                    "quarkus-config-guard: Lint finished with %d error(s) and %d warning(s).",
                    errors, warnings));
        }
    }
}
