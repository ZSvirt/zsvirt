package org.zstack.header.host;

public interface HostConnectionReestablishExtensionPoint {
    void connectionReestablished(HostInventory inv) throws HostException;

    HypervisorType getHypervisorTypeForReestablishExtensionPoint();
}
