package org.zstack.test.integration.kvm.hostallocator

import org.zstack.core.Platform
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.host.HostAllocateExtensionPoint

class HostAllocateExtension implements HostAllocateExtensionPoint {
    private boolean errorOut = false

    boolean isErrorOut() {
        return errorOut
    }

    void setErrorOut(boolean errorOut) {
        this.errorOut = errorOut
    }

    @Override
    void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
        if (isErrorOut()) {
            throw new OperationFailureException(Platform.operr("On purpose"))
        }
    }
}

