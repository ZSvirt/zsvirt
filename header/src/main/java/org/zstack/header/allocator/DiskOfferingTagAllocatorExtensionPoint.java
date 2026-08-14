package org.zstack.header.allocator;

import org.zstack.header.tag.TagInventory;

import java.util.List;

/**
 */
public interface DiskOfferingTagAllocatorExtensionPoint {
    void allocateHost(List<TagInventory> tags, List<HostCandidate> candidates, HostAllocatorSpec spec);
}
