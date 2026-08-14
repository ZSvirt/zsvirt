package org.zstack.header.allocator;

import java.util.List;

/**
 * Created by frank on 7/2/2015.
 */
public interface HostAllocatorFilterExtensionPoint {
    void filter(List<HostCandidate> candidates, HostAllocatorSpec spec);
}
