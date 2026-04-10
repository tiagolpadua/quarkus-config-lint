package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.metadata.MetadataLoader;
import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.model.ExpressionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule QCL003 (WARNING): Expression in a build-time property likely references an environment variable.
 *
 * <p>Heuristic: variable names matching {@code ALL_CAPS_WITH_UNDERSCORES} are likely environment
 * variables. Using them in build-time properties will result in the value being resolved
 * at build time, not at runtime, which is often a mistake.
 *
 * <p>Note: This rule is triggered even if the property is not in the metadata registry,
 * because the pattern is a strong heuristic for environment variable usage that warrants
 * investigation regardless.
 */
public class QCL003EnvVarHeuristicRule implements Rule {

    private static final String RULE_ID = "QCL003";

    private final MetadataLoader metadataLoader;

    public QCL003EnvVarHeuristicRule(MetadataLoader metadataLoader) {
        this.metadataLoader = metadataLoader;
    }

    @Override
    public String getId() {
        return RULE_ID;
    }

    @Override
    public List<Violation> evaluate(ConfigEntry entry) {
        List<Violation> violations = new ArrayList<>();

        if (!entry.hasExpressions()) {
            return violations;
        }

        // Only warn if the property is build-time (or unknown, which may also be build-time)
        if (!metadataLoader.isBuildTime(entry.getPropertyKey())) {
            return violations;
        }

        for (ExpressionInfo expr : entry.getExpressions()) {
            // Only emit QCL003 for expressions that look like env vars
            // (QCL001 already covers the general case, so QCL003 adds a targeted hint)
            if (expr.isEnvironmentVariable()) {
                violations.add(new Violation(
                        RULE_ID,
                        Violation.Severity.WARNING,
                        "Build-time property likely references an environment variable",
                        entry,
                        "The expression '" + expr.getFullExpression() + "' appears to reference an "
                                + "environment variable (ALL_CAPS_WITH_UNDERSCORE pattern). "
                                + "For build-time properties, this variable is read at build time. "
                                + "Changes to this environment variable at runtime will have no effect.",
                        "If you need to control '" + expr.getVariableName()
                                + "' at runtime, check whether the property has a runtime equivalent, "
                                + "or set the explicit value at build time using a Maven property or "
                                + "environment variable in the CI/CD pipeline."
                ));
            }
        }

        return violations;
    }
}
