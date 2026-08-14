package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

public interface FilterVmNicChangeableL3NetworkExtensionPoint {
    List<L3NetworkInventory> filterVmNicChangeableL3Network(VmNicInventory nic, List<L3NetworkInventory> l3s);
}
