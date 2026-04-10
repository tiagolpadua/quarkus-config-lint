package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates all registered lint rules and collects violations.
 */
public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Runs all rules against each config entry and returns all violations.
     *
     * @param entries the config entries to evaluate
     * @return all violations found
     */
    public List<Violation> evaluate(List<ConfigEntry> entries) {
        List<Violation> allViolations = new ArrayList<>();

        for (ConfigEntry entry : entries) {
            for (Rule rule : rules) {
                allViolations.addAll(rule.evaluate(entry));
            }
        }

        return allViolations;
    }
}
