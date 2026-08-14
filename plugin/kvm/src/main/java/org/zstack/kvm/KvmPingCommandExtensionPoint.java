package org.zstack.kvm;

public interface KvmPingCommandExtensionPoint {
    void beforeKvmPing(KVMAgentCommands.PingCmd command);
}
