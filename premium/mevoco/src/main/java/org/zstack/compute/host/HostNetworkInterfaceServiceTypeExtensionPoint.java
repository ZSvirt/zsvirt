package org.zstack.compute.host;

public interface HostNetworkInterfaceServiceTypeExtensionPoint {
    void syncManagementServiceTypeExtensionPoint(String interfaceUuid, boolean isBonding);
}

