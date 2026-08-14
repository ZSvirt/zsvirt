package org.zstack.core.config;

public interface GlobalConfigValidatorExtensionPoint {
    void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException;
}
