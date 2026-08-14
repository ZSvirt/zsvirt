package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceRebootExtensionPoint {
    String preRebootVm(VmInstanceInventory inv);

    void beforeRebootVm(VmInstanceInventory inv);

    void afterRebootVm(VmInstanceInventory inv);

    void failedToRebootVm(VmInstanceInventory inv, ErrorCode reason);
}
