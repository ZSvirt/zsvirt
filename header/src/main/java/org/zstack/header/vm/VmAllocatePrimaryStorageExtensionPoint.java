package org.zstack.header.vm;

import java.util.List;

public interface VmAllocatePrimaryStorageExtensionPoint {
    void filterPrimaryStorageCandidates(VmInstanceSpec spec, List<String> rootPrimaryStorageUuids,
                                        boolean rootPrimaryStorageAutoAllocation);
}
