package org.zstack.header.vm;

import org.zstack.header.network.l3.UsedIpInventory;

import java.util.Map;

/**
 * Created by xing5 on 2016/4/20.
 */
public interface VmIpChangedExtensionPoint {
    void vmIpChanged(VmInstanceInventory vm, VmNicInventory nic, Map<Integer, UsedIpInventory> oldIpMap, Map<Integer, UsedIpInventory> newIpMap);
}
