package org.zstack.header.host;

import org.zstack.header.allocator.HostAllocatorSpec;

/**
 * Created by lining on 2018/3/28.
 */
public interface HostAllocateExtensionPoint {
    void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid);
}
