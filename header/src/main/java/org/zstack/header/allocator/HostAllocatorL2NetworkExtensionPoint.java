package org.zstack.header.allocator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface HostAllocatorL2NetworkExtensionPoint {
    Set<String> getManagedL2NetworkUuids(Collection<String> l2NetworkUuids);

    List<String> getCandidateHostUuids(Collection<String> l2NetworkUuids);
}
