package org.zstack.compute.vm;

import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by GuoYi on 3/18/20.
 */
public interface CleanUpAfterVmFailedToStartExtensionPoint {
    void cleanUpAfterVmFailedToStart(VmInstanceInventory inv, VmOperation op);
}
