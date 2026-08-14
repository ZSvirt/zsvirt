package org.zstack.compute.vm;

public interface VmNicExtensionPoint {
    void afterAddIpAddress(String vmNicUUid, String usedIpUuid);
    void afterDelIpAddress(String vmNicUUid, String usedIpUuid);
}
