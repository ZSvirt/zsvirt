package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface KVMRebootVmExtensionPoint {
    void beforeRebootVmOnKvm(KVMHostInventory host, VmInstanceInventory vm) throws KVMException;
    
    void rebootVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.RebootVmResponse ret);
    
    void rebootVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err);
}
