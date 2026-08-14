package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;

public interface DetachNicExtensionPoint {
    ErrorCode validateDetachNicByDriverTypeAndClusterType(L3NetworkInventory l3, VmInstanceInventory vm);
}
