package org.zstack.header.host;

public interface HostPingTaskExtensionPoint {
    void executeTaskAlongWithPingTask(HostInventory inv);

    HypervisorType getHypervisorType();
}
