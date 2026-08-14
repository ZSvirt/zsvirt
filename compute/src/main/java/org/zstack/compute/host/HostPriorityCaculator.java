package org.zstack.compute.host;

public interface HostPriorityCaculator {
    // The higher return number, the higher priority.
    int getHostConnectPriority(String hostUuid);
}
