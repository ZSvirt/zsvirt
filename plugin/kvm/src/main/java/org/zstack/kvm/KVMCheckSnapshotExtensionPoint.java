package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.host.CheckSnapshotOnHypervisorMsg;

public interface KVMCheckSnapshotExtensionPoint {
    void beforeCheckSnapshot(KVMHostInventory host, CheckSnapshotOnHypervisorMsg msg, KVMAgentCommands.CheckSnapshotCmd cmd, Completion completion);
}
