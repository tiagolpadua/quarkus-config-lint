package io.github.tiagolpadua.quarkusconfigguard.parser;

import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.model.ExpressionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesParserTest {

    private final PropertiesParser parser = new PropertiesParser();

    @TempDir
    Path tempDir;

    private File createFile(String content) throws IOException {
        Path file = tempDir.resolve("application.properties");
        Files.writeString(file, content);
        return file.toFile();
    }

    @Test
    void parsesSimpleKeyValue() throws IOException {
        File f = createFile("quarkus.http.port=8080\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        ConfigEntry entry = entries.get(0);
        assertEquals("quarkus.http.port", entry.getRawKey());
        assertEquals("8080", entry.getRawValue());
        assertEquals("quarkus.http.port", entry.getPropertyKey());
        assertTrue(entry.getProfile().isDefault());
        assertFalse(entry.hasExpressions());
    }

    @Test
    void parsesProfilePrefix() throws IOException {
        File f = createFile("%dev.quarkus.otel.exporter.otlp.enabled=${OTEL_ENABLED:false}\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        ConfigEntry entry = entries.get(0);
        assertEquals("%dev.quarkus.otel.exporter.otlp.enabled", entry.getRawKey());
        assertEquals("${OTEL_ENABLED:false}", entry.getRawValue());
        assertEquals("dev", entry.getProfile().getName());
        assertEquals("quarkus.otel.exporter.otlp.enabled", entry.getPropertyKey());
        assertTrue(entry.hasExpressions());
    }

    @Test
    void parsesExpressionWithoutDefault() throws IOException {
        File f = createFile("quarkus.otel.exporter.otlp.enabled=${OTEL_ENABLED}\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        ConfigEntry entry = entries.get(0);
        assertTrue(entry.hasExpressions());
        ExpressionInfo expr = entry.getExpressions().get(0);
        assertEquals("${OTEL_ENABLED}", expr.getFullExpression());
        assertEquals("OTEL_ENABLED", expr.getVariableName());
        assertNull(expr.getDefaultValue());
        assertTrue(expr.isEnvironmentVariable());
        assertFalse(expr.isPropertyReference());
    }

    @Test
    void parsesExpressionWithDefault() throws IOException {
        File f = createFile("quarkus.otel.exporter.otlp.enabled=${OTEL_ENABLED:false}\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        ExpressionInfo expr = entries.get(0).getExpressions().get(0);
        assertEquals("OTEL_ENABLED", expr.getVariableName());
        assertEquals("false", expr.getDefaultValue());
    }

    @Test
    void parsesPropertyReference() throws IOException {
        File f = createFile("quarkus.otel.exporter.otlp.enabled=${other.property}\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        ExpressionInfo expr = entries.get(0).getExpressions().get(0);
        assertTrue(expr.isPropertyReference());
        assertFalse(expr.isEnvironmentVariable());
    }

    @Test
    void skipsCommentLines() throws IOException {
        File f = createFile("# This is a comment\n! Another comment\nquarkus.http.port=8080\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        assertEquals("quarkus.http.port", entries.get(0).getRawKey());
    }

    @Test
    void skipsEmptyLines() throws IOException {
        File f = createFile("\n\nquarkus.http.port=8080\n\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
    }

    @Test
    void parsesMultipleEntries() throws IOException {
        File f = createFile(
                "quarkus.http.port=8080\n" +
                "quarkus.log.level=INFO\n" +
                "%prod.quarkus.otel.enabled=${OTEL:false}\n"
        );
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(3, entries.size());
        assertEquals("quarkus.http.port", entries.get(0).getPropertyKey());
        assertEquals("quarkus.log.level", entries.get(1).getPropertyKey());
        assertEquals("quarkus.otel.enabled", entries.get(2).getPropertyKey());
        assertEquals("prod", entries.get(2).getProfile().getName());
    }

    @Test
    void detectsMultipleExpressionsInValue() throws IOException {
        File f = createFile("some.key=${VAR1} and ${VAR2}\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        List<ExpressionInfo> expressions = entries.get(0).getExpressions();
        assertEquals(2, expressions.size());
        assertEquals("VAR1", expressions.get(0).getVariableName());
        assertEquals("VAR2", expressions.get(1).getVariableName());
    }

    @Test
    void tracksLineNumbers() throws IOException {
        File f = createFile("# comment\nquarkus.http.port=8080\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(2, entries.get(0).getLineNumber());
    }

    @Test
    void parsesSeparatorWithColon() throws IOException {
        File f = createFile("quarkus.http.port:8080\n");
        List<ConfigEntry> entries = parser.parse(f, "application.properties");

        assertEquals(1, entries.size());
        assertEquals("quarkus.http.port", entries.get(0).getRawKey());
        assertEquals("8080", entries.get(0).getRawValue());
    }
}
