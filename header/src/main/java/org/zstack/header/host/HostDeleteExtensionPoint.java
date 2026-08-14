package org.zstack.header.host;


public interface HostDeleteExtensionPoint {
    void preDeleteHost(HostInventory inventory) throws HostException;

    void beforeDeleteHost(HostInventory inventory);

    void afterDeleteHost(HostInventory inventory);
}
