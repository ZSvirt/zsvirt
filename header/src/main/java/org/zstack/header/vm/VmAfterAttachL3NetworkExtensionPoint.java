package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created by xing5 on 2016/4/18.
 */
public interface VmAfterAttachL3NetworkExtensionPoint {
    void vmAfterAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3);
}
