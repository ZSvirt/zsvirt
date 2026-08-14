package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created by xing5 on 2016/4/18.
 */
public interface VmFailToAttachL3NetworkExtensionPoint {
    void vmFailToAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3, ErrorCode error);
}
