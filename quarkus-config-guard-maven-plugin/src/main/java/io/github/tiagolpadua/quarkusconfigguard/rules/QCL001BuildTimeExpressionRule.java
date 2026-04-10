package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.metadata.MetadataLoader;
import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule QCL001 (ERROR): Build-time property uses expression language {@code ${...}}.
 *
 * <p>Build-time properties are resolved during Quarkus augmentation/build time.
 * Using expression language in their values means the expression is resolved at build
 * time, not at runtime, which leads to silent misconfiguration.
 */
public class QCL001BuildTimeExpressionRule implements Rule {

    private static final String RULE_ID = "QCL001";

    private final MetadataLoader metadataLoader;

    public QCL001BuildTimeExpressionRule(MetadataLoader metadataLoader) {
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

        String displayKey = entry.getRawKey();
        String displayValue = entry.getRawValue();

        violations.add(new Violation(
                RULE_ID,
                Violation.Severity.ERROR,
                "Build-time property uses expression language",
                entry,
                "This property is fixed at build time. Expression language will be resolved during "
                        + "build (augmentation), not at runtime. Environment variables and property "
                        + "references used here will be evaluated once at build time and then "
                        + "become a static value.",
                "Set the value explicitly during the build, or change the property to a runtime "
                        + "property if dynamic configuration is needed. "
                        + "Property: " + displayKey + " = " + displayValue
        ));

        return violations;
    }
}
