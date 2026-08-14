package org.zstack.header.vm;

public interface VmInstanceResumeExtensionPoint {
    void afterResumeVm(VmInstanceInventory inv);
}
