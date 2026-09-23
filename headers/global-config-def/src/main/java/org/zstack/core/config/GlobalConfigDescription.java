package org.zstack.core.config;

/**
 * Immutable definition-side view of a global config. Produced by
 * {@link GlobalConfigDefinitionScanner} without Spring; {@code GlobalConfig}
 * in core is constructed from this at runtime.
 */
public final class GlobalConfigDescription {
    public final String category;
    public final String name;
    public final String type;
    public final String description;
    public final String defaultValue;
    /** From {@code GlobalConfigDef.validatorRegularExpression} or XML; later {@code GlobalConfigValidation}. */
    public final String regularExpression;
    public final GlobalConfigValidation validation;

    public GlobalConfigDescription(String category, String name,
                                   String type, String description, String defaultValue,
                                   String regularExpression,
                                   GlobalConfigValidation validation) {
        if (category == null) {
            throw new IllegalArgumentException("category cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.category = category;
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.regularExpression = (regularExpression == null || regularExpression.isEmpty()) ? null : regularExpression;
        this.validation = validation;
    }

    public String getIdentity() {
        return category + "." + name;
    }

    GlobalConfigDescription mergeXml(GlobalConfigDescription xml) {
        return new GlobalConfigDescription(
                category,
                name,
                type != null ? type : xml.type,
                description != null ? description : xml.description,
                defaultValue != null ? defaultValue : xml.defaultValue,
                regularExpression != null ? regularExpression : xml.regularExpression,
                validation);
    }
}
