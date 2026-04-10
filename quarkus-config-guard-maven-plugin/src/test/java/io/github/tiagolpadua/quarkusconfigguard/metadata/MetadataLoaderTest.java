package io.github.tiagolpadua.quarkusconfigguard.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MetadataLoaderTest {

    private MetadataLoader loader() throws IOException {
        return new MetadataLoader();
    }

    @Test
    void loadsDefaultRegistry() throws IOException {
        MetadataLoader loader = loader();
        assertNotNull(loader);
    }

    @Test
    void knowsBuildTimeProperty() throws IOException {
        MetadataLoader loader = loader();
        assertTrue(loader.isBuildTime("quarkus.otel.exporter.otlp.enabled"));
    }

    @Test
    void knowsRuntimeProperty() throws IOException {
        MetadataLoader loader = loader();
        assertFalse(loader.isBuildTime("quarkus.http.port"));
    }

    @Test
    void returnsNullForUnknownProperty() throws IOException {
        MetadataLoader loader = loader();
        assertNull(loader.getMetadata("quarkus.unknown.property.that.does.not.exist"));
    }

    @Test
    void matchesPrefixWildcard() throws IOException {
        MetadataLoader loader = loader();
        // quarkus.index-dependency.* is in the registry
        assertTrue(loader.isBuildTime("quarkus.index-dependency.my-lib"));
    }

    @Test
    void exactMatchWinsPrefixMatch() throws IOException {
        MetadataLoader loader = loader();
        // quarkus.otel.exporter.otlp.enabled is exact (buildTime=true)
        // quarkus.otel.exporter.otlp.endpoint is exact (buildTime=false)
        assertFalse(loader.isBuildTime("quarkus.otel.exporter.otlp.endpoint"));
    }
}
