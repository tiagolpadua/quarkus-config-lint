package io.github.tiagolpadua.quarkusconfigguard.metadata;

/**
 * Holds metadata about a single Quarkus configuration property.
 */
public class PropertyMetadata {

    private final String key;
    private final boolean buildTime;
    private final String description;

    public PropertyMetadata(String key, boolean buildTime, String description) {
        this.key = key;
        this.buildTime = buildTime;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public boolean isBuildTime() {
        return buildTime;
    }

    public String getDescription() {
        return description;
    }
}
