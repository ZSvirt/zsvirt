package org.zstack.kvm;

public interface KVMPreUpdateNicExtensionPoint {
    void preUpdateNic(KVMHostInventory host, KVMAgentCommands.UpdateNicCmd cmd);
}
