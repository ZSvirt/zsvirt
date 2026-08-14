package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

import java.util.List;

/**
 * Created by wenhao on 2021/1/27.
 */
public interface FilterAttachableL3NetworkExtensionPoint {
    
    List<L3NetworkInventory> filterAttachableL3Network(VmInstanceInventory vm, List<L3NetworkInventory> l3s);
    
}
