package org.zstack.core.config;

import org.zstack.header.identity.SessionInventory;

public interface GlobalConfigBeforeResetExtensionPoint {
    void beforeResetExtensionPoint(SessionInventory session);
}
