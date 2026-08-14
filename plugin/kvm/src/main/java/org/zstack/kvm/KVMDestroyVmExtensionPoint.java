package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface KVMDestroyVmExtensionPoint {
    void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) throws KVMException;

    void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd);
    
    void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm);
    
    void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err);
}
