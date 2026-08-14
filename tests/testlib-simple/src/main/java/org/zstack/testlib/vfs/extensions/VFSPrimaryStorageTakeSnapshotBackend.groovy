package org.zstack.testlib.vfs.extensions

import org.springframework.http.HttpEntity
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.volume.VolumeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.vfs.Qcow2

interface VFSPrimaryStorageTakeSnapshotBackend {
    String getPrimaryStorageType()

    /**
     *
     * @param cmd
     * @param volume
     * @return the new install path of the volume
     */
    VFSSnapshot takeSnapshot(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume)

    void mergeSnapshots(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.MergeSnapshotCmd cmd, VolumeInventory volume)

    List<TakeSnapshotsOnKvmResultStruct> takeSnapshotsOnVolumes(String primaryStorageUuid, HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs)

    void blockStream(HttpEntity<String> e, EnvSpec spec, VolumeInventory volume)

    Qcow2 blockCommit(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockCommitCmd cmd, VolumeInventory volume)

    Qcow2 blockPull(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockPullCmd cmd, VolumeInventory volume)
}
