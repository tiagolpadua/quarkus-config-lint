# quarkus-config-guard-maven-plugin

A Maven plugin that performs static analysis (lint) on Quarkus configuration files to detect misuses of build-time properties.

## Problem

Some Quarkus properties are **fixed at build time** (during augmentation). Using expression language
(`${ENV_VAR}`, `${ENV_VAR:default}`, `${other.property}`) in their values is problematic because:

- The expression is resolved **at build time**, not at runtime.
- Changes to environment variables or runtime properties will have **no effect** after the binary is built.
- This leads to **silent misconfiguration** in production.

**Example of a violation:**

```properties
# ❌ BAD: quarkus.otel.exporter.otlp.enabled is fixed at build time
%dev.quarkus.otel.exporter.otlp.enabled=${OTEL_EXPORTER_OTLP_ENABLED:false}

# ✅ OK: Set explicitly during build
%dev.quarkus.otel.exporter.otlp.enabled=false
```

## Plugin Setup

Add the plugin to your Quarkus project's `pom.xml`:

```xml
<plugin>
  <groupId>io.github.tiagolpadua</groupId>
  <artifactId>quarkus-config-guard-maven-plugin</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>lint</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <!-- Fail the build on ERROR-level violations (default: true) -->
    <failOnError>true</failOnError>
    <!-- Fail the build on WARNING-level violations (default: false) -->
    <failOnWarning>false</failOnWarning>
  </configuration>
</plugin>
```

The `lint` goal runs in the `validate` phase by default.

## Lint Rules

| Rule   | Severity | Description |
|--------|----------|-------------|
| QCL001 | ERROR    | Build-time property uses expression language `${...}` |
| QCL002 | WARNING  | Build-time property references another property `${some.property}` |
| QCL003 | WARNING  | Build-time property likely references an environment variable (ALL_CAPS pattern) |

## Example Output

```
[ERROR] QCL001: Build-time property uses expression language
        %dev.quarkus.otel.exporter.otlp.enabled=${OTEL_EXPORTER_OTLP_ENABLED:false}
        key: quarkus.otel.exporter.otlp.enabled
        file: application.properties (line 5)
        reason: This property is fixed at build time. Expression language will be resolved during
                build (augmentation), not at runtime.
        suggestion: Set the value explicitly during the build, or change the property to a runtime
                    property if dynamic configuration is needed.
```

## Configuration Parameters

| Parameter                          | Default | Description |
|------------------------------------|---------|-------------|
| `quarkus.config.guard.failOnError` | `true`  | Fail build on ERROR violations |
| `quarkus.config.guard.failOnWarning` | `false` | Fail build on WARNING violations |
| `quarkus.config.guard.skip`        | `false` | Skip the lint check entirely |

Parameters can also be passed on the command line:

```bash
mvn validate -Dquarkus.config.guard.skip=true
```

## Project Structure

```
quarkus-config-guard-maven-plugin/
├── src/main/java/.../
│   ├── LintMojo.java              # Maven Mojo entry point
│   ├── model/
│   │   ├── ConfigEntry.java       # A single parsed property entry
│   │   ├── Profile.java           # Quarkus profile (%dev, %prod, etc.)
│   │   └── ExpressionInfo.java    # Parsed ${...} expression
│   ├── parser/
│   │   └── PropertiesParser.java  # Reads application.properties files
│   ├── metadata/
│   │   ├── PropertyMetadata.java  # Metadata for a single property
│   │   └── MetadataLoader.java    # Loads JSON property registry
│   ├── rules/
│   │   ├── Rule.java              # Rule interface
│   │   ├── Violation.java         # A lint finding
│   │   ├── RuleEngine.java        # Orchestrates rules
│   │   ├── QCL001BuildTimeExpressionRule.java
│   │   ├── QCL002PropertyReferenceRule.java
│   │   └── QCL003EnvVarHeuristicRule.java
│   └── report/
│       └── ConsoleReporter.java   # Formats and prints violations
└── src/main/resources/
    └── quarkus-properties-metadata.json  # Built-in property metadata registry
```

## Extending the Metadata Registry

The built-in registry (`quarkus-properties-metadata.json`) contains common Quarkus properties.
Entries support exact keys and wildcard prefixes:

```json
{
  "quarkus.otel.exporter.otlp.enabled": {
    "buildTime": true,
    "description": "Whether the OTLP exporter is enabled."
  },
  "quarkus.index-dependency.*": {
    "buildTime": true,
    "description": "Additional indexed dependencies."
  }
}
```

## Building

```bash
mvn clean install
```

## Running Tests

```bash
mvn test
```
