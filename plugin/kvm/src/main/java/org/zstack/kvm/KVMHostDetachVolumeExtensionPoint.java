package org.zstack.kvm;

public interface KVMHostDetachVolumeExtensionPoint {
    void afterDetachVolume(String hostUuid);
}

