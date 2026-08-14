package org.zstack.header.vm;

public interface VmInstanceAttachNicExtensionPoint {
    void afterAttachNicToVm(VmNicInventory nic);
}
