package io.github.tiagolpadua.quarkusconfigguard.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Loads property metadata from the bundled JSON registry.
 *
 * <p>The registry is a JSON file with the following structure:
 * <pre>
 * {
 *   "quarkus.otel.exporter.otlp.enabled": {
 *     "buildTime": true,
 *     "description": "..."
 *   }
 * }
 * </pre>
 *
 * <p>Wildcard keys ending in {@code .*} match any sub-property of that prefix.
 * For example, {@code quarkus.otel.*} matches {@code quarkus.otel.exporter.otlp.enabled}.
 */
public class MetadataLoader {

    private static final String DEFAULT_REGISTRY_PATH = "/quarkus-properties-metadata.json";

    private final Map<String, PropertyMetadata> exactRegistry = new HashMap<>();
    private final Map<String, PropertyMetadata> prefixRegistry = new HashMap<>();

    /**
     * Creates a loader using the bundled default metadata registry.
     *
     * @throws IOException if the registry cannot be read
     */
    public MetadataLoader() throws IOException {
        this(DEFAULT_REGISTRY_PATH);
    }

    /**
     * Creates a loader using a custom registry resource path.
     *
     * @param resourcePath classpath resource path to the JSON registry
     * @throws IOException if the registry cannot be read
     */
    public MetadataLoader(String resourcePath) throws IOException {
        try (InputStream is = MetadataLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Metadata registry not found on classpath: " + resourcePath);
            }
            loadFromStream(is);
        }
    }

    private void loadFromStream(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(is);

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode node = entry.getValue();

            boolean buildTime = node.path("buildTime").asBoolean(false);
            String description = node.path("description").asText(null);

            PropertyMetadata metadata = new PropertyMetadata(key, buildTime, description);

            if (key.endsWith(".*")) {
                String prefix = key.substring(0, key.length() - 2);
                prefixRegistry.put(prefix, metadata);
            } else {
                exactRegistry.put(key, metadata);
            }
        }
    }

    /**
     * Looks up metadata for the given property key.
     *
     * <p>First checks for an exact match, then checks prefix wildcards from most to least specific.
     *
     * @param propertyKey the property key without profile prefix
     * @return metadata if found, or {@code null} if the property is not in the registry
     */
    public PropertyMetadata getMetadata(String propertyKey) {
        // Exact match first
        PropertyMetadata exact = exactRegistry.get(propertyKey);
        if (exact != null) {
            return exact;
        }

        // Prefix wildcard match — find the longest matching prefix
        PropertyMetadata bestMatch = null;
        int bestMatchLength = -1;

        for (Map.Entry<String, PropertyMetadata> entry : prefixRegistry.entrySet()) {
            String prefix = entry.getKey();
            if (propertyKey.startsWith(prefix + ".") || propertyKey.equals(prefix)) {
                if (prefix.length() > bestMatchLength) {
                    bestMatchLength = prefix.length();
                    bestMatch = entry.getValue();
                }
            }
        }

        return bestMatch;
    }

    /**
     * Returns whether the property is known to be a build-time property.
     *
     * @param propertyKey the property key without profile prefix
     * @return {@code true} if the property is build-time, {@code false} otherwise
     */
    public boolean isBuildTime(String propertyKey) {
        PropertyMetadata metadata = getMetadata(propertyKey);
        return metadata != null && metadata.isBuildTime();
    }
}
