package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.PullVolumeSnapshotOnHypervisorMsg;
import org.zstack.header.host.PullVolumeSnapshotOnHypervisorReply;

public interface KVMBlockPullExtensionPoint {
    void beforePullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, Completion completion);

    void afterPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, PullVolumeSnapshotOnHypervisorReply reply, Completion completion);

    void failedToPullVolume(KVMHostInventory host, PullVolumeSnapshotOnHypervisorMsg msg, KVMAgentCommands.BlockPullCmd cmd, KVMAgentCommands.BlockPullResponse rsp, ErrorCode err);
}
