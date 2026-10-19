package org.zstack.header.storage.primary.allocate;

import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;

import java.util.List;

public interface PrimaryStorageAllocatorFilterExtensionPoint {
    void filter(List<PrimaryStorageCandidate> candidates, PrimaryStorageAllocationSpec spec);
}
