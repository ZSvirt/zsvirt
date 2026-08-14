package org.zstack.header.allocator;

public interface AttachVmInstanceToAffinityGroupExtensionPoint {
    void attachVmInstanceToAffinityGroup(String vmUuid, String applianceType);
    void detachVmInstanceFromAffinityGroup(String vmUuid);
}
