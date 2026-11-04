package org.zstack.core.config;

/**
 * Identity of a global config instance, without Spring or {@code GlobalConfig}.
 * {@code GlobalConfig} implements this so {@link GlobalConfigDefinitionScanner}
 * can {@code field.get(null)} without depending on core.
 * <p>
 * Named {@code ConfigDefinition} because {@link GlobalConfigDefinition} is already
 * the holder-class annotation.
 */
public interface ConfigDefinition {
    String getCategory();
    String getName();
}
