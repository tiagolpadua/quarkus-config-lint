package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;

import java.util.List;

/**
 * Interface for a lint rule that analyses a {@link ConfigEntry} and returns violations.
 */
public interface Rule {

    /**
     * Returns the rule identifier (e.g., "QCL001").
     */
    String getId();

    /**
     * Evaluates the rule against the given config entry.
     *
     * @param entry the config entry to check
     * @return list of violations found (empty if none)
     */
    List<Violation> evaluate(ConfigEntry entry);
}
