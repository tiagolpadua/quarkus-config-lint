package io.github.tiagolpadua.quarkusconfigguard;

import io.github.tiagolpadua.quarkusconfigguard.metadata.MetadataLoader;
import io.github.tiagolpadua.quarkusconfigguard.model.ConfigEntry;
import io.github.tiagolpadua.quarkusconfigguard.parser.PropertiesParser;
import io.github.tiagolpadua.quarkusconfigguard.report.ConsoleReporter;
import io.github.tiagolpadua.quarkusconfigguard.rules.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven Mojo that lints Quarkus {@code application.properties} files to detect
 * misuses of build-time properties, such as assigning expression language values
 * (e.g., {@code ${ENV_VAR}}) to properties that are fixed at build time.
 *
 * <p>Binds to the {@code validate} phase by default.
 *
 * <p>Example configuration:
 * <pre>{@code
 * <plugin>
 *   <groupId>io.github.tiagolpadua</groupId>
 *   <artifactId>quarkus-config-guard-maven-plugin</artifactId>
 *   <version>1.0.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals>
 *         <goal>lint</goal>
 *       </goals>
 *     </execution>
 *   </executions>
 *   <configuration>
 *     <failOnError>true</failOnError>
 *     <failOnWarning>false</failOnWarning>
 *   </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class LintMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Whether to fail the build when ERROR-level violations are found.
     * Default: {@code true}
     */
    @Parameter(property = "quarkus.config.guard.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Whether to fail the build when WARNING-level violations are found.
     * Default: {@code false}
     */
    @Parameter(property = "quarkus.config.guard.failOnWarning", defaultValue = "false")
    private boolean failOnWarning;

    /**
     * Whether to skip the lint check entirely.
     * Default: {@code false}
     */
    @Parameter(property = "quarkus.config.guard.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Base directory for resource files (defaults to src/main/resources).
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
    private File resourcesDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("quarkus-config-guard: Skipping lint (skip=true).");
            return;
        }

        // Resolve the resources directory
        File resDir = resolveResourcesDirectory();
        if (!resDir.exists() || !resDir.isDirectory()) {
            getLog().info("quarkus-config-guard: Resources directory not found, skipping: " + resDir);
            return;
        }

        getLog().info("quarkus-config-guard: Scanning " + resDir.getAbsolutePath());

        // Collect all application.properties files
        List<File> propertyFiles = collectPropertyFiles(resDir);
        if (propertyFiles.isEmpty()) {
            getLog().info("quarkus-config-guard: No application.properties files found.");
            return;
        }

        getLog().info("quarkus-config-guard: Found " + propertyFiles.size() + " properties file(s).");

        // Set up infrastructure
        MetadataLoader metadataLoader;
        try {
            metadataLoader = new MetadataLoader();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load property metadata registry", e);
        }

        PropertiesParser parser = new PropertiesParser();
        List<Rule> rules = List.of(
                new QCL001BuildTimeExpressionRule(metadataLoader),
                new QCL002PropertyReferenceRule(metadataLoader),
                new QCL003EnvVarHeuristicRule(metadataLoader)
        );
        RuleEngine ruleEngine = new RuleEngine(rules);
        ConsoleReporter reporter = new ConsoleReporter(getLog());

        // Parse all files
        List<ConfigEntry> allEntries = new ArrayList<>();
        for (File file : propertyFiles) {
            String relativePath = resDir.toPath().relativize(file.toPath()).toString();
            getLog().debug("quarkus-config-guard: Parsing " + relativePath);
            try {
                List<ConfigEntry> entries = parser.parse(file, relativePath);
                allEntries.addAll(entries);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse properties file: " + file, e);
            }
        }

        // Run rules
        List<Violation> violations = ruleEngine.evaluate(allEntries);

        // Report
        reporter.report(violations);
        reporter.reportSummary(violations);

        // Determine failure
        long errorCount = violations.stream().filter(Violation::isError).count();
        long warningCount = violations.stream().filter(Violation::isWarning).count();

        if (failOnError && errorCount > 0) {
            throw new MojoFailureException(
                    "quarkus-config-guard: Build failed due to " + errorCount + " error(s). "
                            + "Fix the reported QCL violations or set failOnError=false to suppress.");
        }

        if (failOnWarning && warningCount > 0) {
            throw new MojoFailureException(
                    "quarkus-config-guard: Build failed due to " + warningCount + " warning(s). "
                            + "Fix the reported QCL violations or set failOnWarning=false to suppress.");
        }
    }

    /**
     * Resolves the resources directory. Falls back to the standard Maven resources directory.
     */
    private File resolveResourcesDirectory() {
        // Prefer the standard Maven resources directory
        if (project != null) {
            List<String> resourceRoots = project.getBuild().getResources()
                    .stream()
                    .map(r -> r.getDirectory())
                    .toList();
            for (String root : resourceRoots) {
                File dir = new File(root);
                if (dir.exists() && dir.isDirectory()) {
                    return dir;
                }
            }
            // Fallback: standard location
            return new File(project.getBasedir(), "src/main/resources");
        }
        return resourcesDirectory;
    }

    /**
     * Collects all application.properties and application-*.properties files from the directory.
     */
    private List<File> collectPropertyFiles(File directory) {
        List<File> result = new ArrayList<>();
        File[] files = directory.listFiles(f ->
                f.isFile() && f.getName().matches("application(-[^.]+)?\\.properties"));
        if (files != null) {
            for (File f : files) {
                result.add(f);
            }
        }
        return result;
    }
}
