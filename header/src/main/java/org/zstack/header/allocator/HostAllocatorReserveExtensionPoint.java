package org.zstack.header.allocator;

import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;

import java.util.List;

/**
 * Created by shixin.ruan on 28/02/2018.
 */
public interface HostAllocatorReserveExtensionPoint extends Flow {
    public HostAllocatorReserveExtensionPoint getExtension();
}
