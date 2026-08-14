package org.zstack.core.externalservice;

import org.zstack.header.core.external.service.ExternalServiceCapabilities;

public class ExternalServiceCapabilitiesBuilder extends ExternalServiceCapabilities {
    public static ExternalServiceCapabilitiesBuilder build() {
        return new ExternalServiceCapabilitiesBuilder();
    }

    public ExternalServiceCapabilitiesBuilder reloadConfig(boolean reloadConfig) {
        setReloadConfig(reloadConfig);
        return this;
    }
}
