package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.metadata.MetadataLoader;
import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.model.ExpressionInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule QCL002 (WARNING): Build-time property references another property via {@code ${some.other.property}}.
 *
 * <p>While this may work at build time if the referenced property is also defined,
 * it creates a fragile dependency that is not obvious and may break with profile changes.
 */
public class QCL002PropertyReferenceRule implements Rule {

    private static final String RULE_ID = "QCL002";

    private final MetadataLoader metadataLoader;

    public QCL002PropertyReferenceRule(MetadataLoader metadataLoader) {
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

        if (!metadataLoader.isBuildTime(entry.getPropertyKey())) {
            return violations;
        }

        for (ExpressionInfo expr : entry.getExpressions()) {
            if (expr.isPropertyReference()) {
                violations.add(new Violation(
                        RULE_ID,
                        Violation.Severity.WARNING,
                        "Build-time property references another property",
                        entry,
                        "The expression '" + expr.getFullExpression() + "' references another property. "
                                + "For build-time properties this reference is resolved during build/augmentation, "
                                + "not at runtime. The referenced property must be available at build time.",
                        "Ensure the referenced property '" + expr.getVariableName()
                                + "' is explicitly set at build time, or refactor to avoid "
                                + "property chaining on build-time properties."
                ));
            }
        }

        return violations;
    }
}
