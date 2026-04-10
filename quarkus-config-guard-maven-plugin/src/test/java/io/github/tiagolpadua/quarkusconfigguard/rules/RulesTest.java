package io.github.tiagolpadua.quarkusconfigguard.rules;

import io.github.tiagolpadua.quarkusconfigguard.metadata.MetadataLoader;
import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.model.ExpressionInfo;
import io.github.tiagolpadua.quarkusconfigguard.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RulesTest {

    private MetadataLoader metadataLoader;

    @BeforeEach
    void setUp() throws IOException {
        metadataLoader = new MetadataLoader();
    }

    // --- Helpers ---

    private ConfigEntry entry(String rawKey, String rawValue, Profile profile, String propertyKey, List<ExpressionInfo> expressions) {
        return new ConfigEntry(rawKey, rawValue, profile, propertyKey, "application.properties", 1, expressions);
    }

    private ExpressionInfo envVarExpr(String varName) {
        return new ExpressionInfo("${" + varName + "}", varName, null);
    }

    private ExpressionInfo envVarExprWithDefault(String varName, String def) {
        return new ExpressionInfo("${" + varName + ":" + def + "}", varName, def);
    }

    private ExpressionInfo propRefExpr(String propName) {
        return new ExpressionInfo("${" + propName + "}", propName, null);
    }

    // --- QCL001 tests ---

    @Test
    void qcl001_detects_buildtime_property_with_env_var_expression() {
        ConfigEntry entry = entry(
                "%dev.quarkus.otel.exporter.otlp.enabled",
                "${OTEL_EXPORTER_OTLP_ENABLED:false}",
                new Profile("dev"),
                "quarkus.otel.exporter.otlp.enabled",
                List.of(envVarExprWithDefault("OTEL_EXPORTER_OTLP_ENABLED", "false"))
        );

        QCL001BuildTimeExpressionRule rule = new QCL001BuildTimeExpressionRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertEquals(1, violations.size());
        assertEquals("QCL001", violations.get(0).getRuleId());
        assertEquals(Violation.Severity.ERROR, violations.get(0).getSeverity());
    }

    @Test
    void qcl001_no_violation_for_runtime_property() {
        ConfigEntry entry = entry(
                "quarkus.http.port",
                "${HTTP_PORT:8080}",
                Profile.DEFAULT,
                "quarkus.http.port",
                List.of(envVarExprWithDefault("HTTP_PORT", "8080"))
        );

        QCL001BuildTimeExpressionRule rule = new QCL001BuildTimeExpressionRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertTrue(violations.isEmpty());
    }

    @Test
    void qcl001_no_violation_without_expressions() {
        ConfigEntry entry = entry(
                "quarkus.otel.exporter.otlp.enabled",
                "true",
                Profile.DEFAULT,
                "quarkus.otel.exporter.otlp.enabled",
                List.of()
        );

        QCL001BuildTimeExpressionRule rule = new QCL001BuildTimeExpressionRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertTrue(violations.isEmpty());
    }

    // --- QCL002 tests ---

    @Test
    void qcl002_detects_property_reference_in_buildtime_property() {
        ConfigEntry entry = entry(
                "quarkus.otel.exporter.otlp.enabled",
                "${some.other.property}",
                Profile.DEFAULT,
                "quarkus.otel.exporter.otlp.enabled",
                List.of(propRefExpr("some.other.property"))
        );

        QCL002PropertyReferenceRule rule = new QCL002PropertyReferenceRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertEquals(1, violations.size());
        assertEquals("QCL002", violations.get(0).getRuleId());
        assertEquals(Violation.Severity.WARNING, violations.get(0).getSeverity());
    }

    @Test
    void qcl002_no_violation_for_env_var_expression() {
        ConfigEntry entry = entry(
                "quarkus.otel.exporter.otlp.enabled",
                "${OTEL_ENABLED:false}",
                Profile.DEFAULT,
                "quarkus.otel.exporter.otlp.enabled",
                List.of(envVarExprWithDefault("OTEL_ENABLED", "false"))
        );

        QCL002PropertyReferenceRule rule = new QCL002PropertyReferenceRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertTrue(violations.isEmpty());
    }

    // --- QCL003 tests ---

    @Test
    void qcl003_detects_env_var_heuristic_in_buildtime_property() {
        ConfigEntry entry = entry(
                "quarkus.otel.exporter.otlp.enabled",
                "${OTEL_ENABLED}",
                Profile.DEFAULT,
                "quarkus.otel.exporter.otlp.enabled",
                List.of(envVarExpr("OTEL_ENABLED"))
        );

        QCL003EnvVarHeuristicRule rule = new QCL003EnvVarHeuristicRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertEquals(1, violations.size());
        assertEquals("QCL003", violations.get(0).getRuleId());
        assertEquals(Violation.Severity.WARNING, violations.get(0).getSeverity());
    }

    @Test
    void qcl003_no_violation_for_runtime_property() {
        ConfigEntry entry = entry(
                "quarkus.http.port",
                "${HTTP_PORT:8080}",
                Profile.DEFAULT,
                "quarkus.http.port",
                List.of(envVarExprWithDefault("HTTP_PORT", "8080"))
        );

        QCL003EnvVarHeuristicRule rule = new QCL003EnvVarHeuristicRule(metadataLoader);
        List<Violation> violations = rule.evaluate(entry);

        assertTrue(violations.isEmpty());
    }

    // --- RuleEngine tests ---

    @Test
    void ruleEngine_aggregatesViolationsFromAllRules() {
        // This entry should trigger QCL001 (ERROR) and QCL003 (WARNING) for an env var
        ConfigEntry entry = entry(
                "%prod.quarkus.otel.exporter.otlp.enabled",
                "${OTEL_EXPORTER_OTLP_ENABLED:false}",
                new Profile("prod"),
                "quarkus.otel.exporter.otlp.enabled",
                List.of(envVarExprWithDefault("OTEL_EXPORTER_OTLP_ENABLED", "false"))
        );

        RuleEngine engine = new RuleEngine(List.of(
                new QCL001BuildTimeExpressionRule(metadataLoader),
                new QCL002PropertyReferenceRule(metadataLoader),
                new QCL003EnvVarHeuristicRule(metadataLoader)
        ));

        List<Violation> violations = engine.evaluate(List.of(entry));

        assertTrue(violations.stream().anyMatch(v -> v.getRuleId().equals("QCL001")));
        assertTrue(violations.stream().anyMatch(v -> v.getRuleId().equals("QCL003")));
        assertTrue(violations.stream().noneMatch(v -> v.getRuleId().equals("QCL002")));
    }

    @Test
    void ruleEngine_returnsEmptyForCleanEntry() {
        ConfigEntry entry = entry(
                "quarkus.http.port",
                "8080",
                Profile.DEFAULT,
                "quarkus.http.port",
                List.of()
        );

        RuleEngine engine = new RuleEngine(List.of(
                new QCL001BuildTimeExpressionRule(metadataLoader),
                new QCL002PropertyReferenceRule(metadataLoader),
                new QCL003EnvVarHeuristicRule(metadataLoader)
        ));

        List<Violation> violations = engine.evaluate(List.of(entry));
        assertTrue(violations.isEmpty());
    }
}
