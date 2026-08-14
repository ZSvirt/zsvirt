package org.zstack.header.core.external.service;

public class ExternalServiceCapabilities {
    private boolean reloadConfig = false;

    public boolean isReloadConfig() {
        return reloadConfig;
    }

    public void setReloadConfig(boolean reloadConfig) {
        this.reloadConfig = reloadConfig;
    }
}
