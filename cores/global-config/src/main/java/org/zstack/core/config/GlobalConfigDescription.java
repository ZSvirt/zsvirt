package org.zstack.core.config;

/**
 * Immutable definition-side view of a global config. Produced by
 * {@link GlobalConfigDefinitionScanner} without Spring; {@code GlobalConfig}
 * in core is constructed from this at runtime.
 */
public final class GlobalConfigDescription {
    public final String category;
    public final String name;
    public final GlobalConfigDef def;
    public final GlobalConfigValidation validation;

    public GlobalConfigDescription(String category, String name,
                                   GlobalConfigDef def, GlobalConfigValidation validation) {
        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.category = category;
        this.name = name;
        this.def = def;
        this.validation = validation;
    }

    public String getIdentity() {
        return category + "." + name;
    }
}
