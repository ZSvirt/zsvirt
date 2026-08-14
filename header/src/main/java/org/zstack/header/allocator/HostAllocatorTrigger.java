package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface HostAllocatorTrigger {
    void next();

    void fail(ErrorCode errorCode);
}
