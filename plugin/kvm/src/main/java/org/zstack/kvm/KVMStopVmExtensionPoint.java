package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface KVMStopVmExtensionPoint {
    void beforeStopVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.StopVmCmd cmd) throws KVMException;
    
    void stopVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm);
    
    void stopVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err);
}
