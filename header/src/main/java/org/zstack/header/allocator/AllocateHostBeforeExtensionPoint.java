package org.zstack.header.allocator;

public interface AllocateHostBeforeExtensionPoint {
    void beforeAllocateHost(AllocateHostMsg msg);
}
