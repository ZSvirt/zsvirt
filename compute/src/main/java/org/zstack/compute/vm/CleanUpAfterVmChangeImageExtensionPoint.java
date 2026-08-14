package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceInventory;

public interface CleanUpAfterVmChangeImageExtensionPoint {
    void cleanUpAfterVmChangeImage(VmInstanceInventory inv);
}
