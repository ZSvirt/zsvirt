package org.zstack.kvm;

public interface KvmPreDetachNicExtensionPoint {
    void preDetachNicExtensionPoint(KVMHostInventory host, KVMAgentCommands.DetachNicCommand cmd);
}
