package org.zstack.header.vm;

import org.zstack.header.core.Completion;

public interface VmAttachOtherDiskExtensionPoint {
    void attachOtherDiskToVm(DiskAO diskAO, String vmInstanceUuid, Completion completion);
    String getDiskType();
}
