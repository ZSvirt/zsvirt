package org.zstack.header.allocator;

/**
 * Created by frank on 10/24/2015.
 */
public interface HostAllocatorStrategyExtensionPoint {
    String getHostAllocatorStrategyName(HostAllocatorSpec spec);
}
