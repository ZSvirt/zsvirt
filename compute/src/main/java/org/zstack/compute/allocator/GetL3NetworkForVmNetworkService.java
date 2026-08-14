package org.zstack.compute.allocator;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

public interface GetL3NetworkForVmNetworkService {
    List<L3NetworkInventory> getL3NetworkForVmNetworkService(VmInstanceInventory vm);
}
