package org.zstack.header.storage.addon.primary;

import org.zstack.header.host.HypervisorType;

public interface NodeHealthyCheckProtocolExtensionPoint {
    HypervisorType getHypervisorType();
    String getHealthyProtocol();
}
