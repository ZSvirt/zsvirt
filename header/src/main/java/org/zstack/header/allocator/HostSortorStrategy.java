package org.zstack.header.allocator;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
public interface HostSortorStrategy {
    void sort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<HostInventory> completion);

    void dryRunSort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<List<HostInventory>> completion);
}
