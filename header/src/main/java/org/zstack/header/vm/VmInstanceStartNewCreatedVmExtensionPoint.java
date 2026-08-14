package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

public interface VmInstanceStartNewCreatedVmExtensionPoint {
    String preStartNewCreatedVm(VmInstanceInventory inv);

    void beforeStartNewCreatedVm(VmInstanceInventory inv);

    void afterStartNewCreatedVm(VmInstanceInventory inv);

    void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason);
}
