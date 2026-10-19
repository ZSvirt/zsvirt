package org.zstack.header.storage.primary.allocate;

import org.zstack.header.errorcode.ErrorCode;

public interface PrimaryStorageAllocatorTrigger {
    void next();

    void fail(ErrorCode errorCode);
}
