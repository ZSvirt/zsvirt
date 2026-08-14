package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceStartExtensionPoint {
    String preStartVm(VmInstanceInventory inv);

    void beforeStartVm(VmInstanceInventory inv);

    void afterStartVm(VmInstanceInventory inv);

    void failedToStartVm(VmInstanceInventory inv, ErrorCode reason);
}
