package io.github.tiagolpadua.quarkusconfigguard.model;

/**
 * Represents a parsed Quarkus profile prefix (e.g., %dev, %prod).
 */
public class Profile {

    private final String name;

    public static final Profile DEFAULT = new Profile(null);

    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return name == null;
    }

    @Override
    public String toString() {
        return name == null ? "<default>" : name;
    }
}
