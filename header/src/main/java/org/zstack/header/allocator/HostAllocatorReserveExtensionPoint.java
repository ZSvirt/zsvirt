package org.zstack.header.allocator;

import org.zstack.header.core.workflow.Flow;

/**
 * Created by shixin.ruan on 28/02/2018.
 */
public interface HostAllocatorReserveExtensionPoint extends Flow {
    HostAllocatorReserveExtensionPoint getExtension();
}
