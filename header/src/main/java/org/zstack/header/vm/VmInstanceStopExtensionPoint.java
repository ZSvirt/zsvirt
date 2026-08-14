package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceStopExtensionPoint {
    String preStopVm(VmInstanceInventory inv);

    void beforeStopVm(VmInstanceInventory inv);

    void afterStopVm(VmInstanceInventory inv);

    void failedToStopVm(VmInstanceInventory inv, ErrorCode reason);
}
