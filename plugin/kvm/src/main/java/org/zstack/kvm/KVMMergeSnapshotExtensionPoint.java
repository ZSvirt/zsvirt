package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;


public interface KVMMergeSnapshotExtensionPoint {
    void beforeMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd);

    void afterMergeSnapshot(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd);

    void afterMergeSnapshotFailed(KVMHostInventory host, MergeVolumeSnapshotOnKvmMsg msg, KVMAgentCommands.MergeSnapshotCmd cmd, ErrorCode err);
}
