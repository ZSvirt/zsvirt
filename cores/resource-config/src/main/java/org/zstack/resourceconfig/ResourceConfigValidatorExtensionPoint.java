package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfigException;

public interface ResourceConfigValidatorExtensionPoint {
    void validateResourceConfig(String resourceUuid, String oldValue, String newValue) throws GlobalConfigException;
}
