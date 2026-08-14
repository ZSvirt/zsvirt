package org.zstack.kvm;

public interface KVMHostAttachVolumeExtensionPoint {
    void afterAttachVolume(String hostUuid);
}
