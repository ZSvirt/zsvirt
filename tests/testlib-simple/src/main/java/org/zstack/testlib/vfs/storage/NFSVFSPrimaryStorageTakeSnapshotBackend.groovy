package org.zstack.testlib.vfs.storage

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.NfsPrimaryStorageSpec
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.testlib.vfs.extensions.VFSSnapshot

class NFSVFSPrimaryStorageTakeSnapshotBackend implements AbstractFileSystemBasedVFSPrimaryStorageBackend, VFSPrimaryStorageTakeSnapshotBackend {
    @Override
    String getPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE
    }

    @Override
    VFSSnapshot takeSnapshot(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume) {
        String primaryStorageUuid = Q.New(VolumeVO.class)
                .select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.uuid, cmd.volumeUuid)
                .findValue()
        VFS vfs = NfsPrimaryStorageSpec.vfs(primaryStorageUuid, spec)
        if (cmd.isOnline()) {
            vfs.Assert(vfs.exists(cmd.installPath), "cannot find file[${cmd.installPath}]")
            vfs.delete(cmd.installPath)
        }

        return doTakeSnapshot(vfs, cmd, volume)
    }

    @Override
    void mergeSnapshots(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.MergeSnapshotCmd cmd, VolumeInventory volume) {
    }

    @Override
    List<TakeSnapshotsOnKvmResultStruct> takeSnapshotsOnVolumes(String primaryStorageUuid, HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        return doTakeSnapshotsOnVolumes(NfsPrimaryStorageSpec.vfs(primaryStorageUuid, spec), snapshotJobs)
    }

    @Override
    void blockStream(HttpEntity<String> e, EnvSpec spec, VolumeInventory volume) {
        VFS vfs = NfsPrimaryStorageSpec.vfs(volume.getPrimaryStorageUuid(), spec)
        if (vfs.exists(volume.getInstallPath())) {
            vfs.delete(volume.getInstallPath())
        }
        blockStream(vfs, volume)
    }

    @Override
    Qcow2 blockCommit(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockCommitCmd cmd, VolumeInventory volume) {
        String primaryStorageUuid = Q.New(VolumeVO.class).select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.uuid, volume.uuid).findValue()
        VFS vfs = NfsPrimaryStorageSpec.vfs(primaryStorageUuid, spec)
        Qcow2 top = vfs.getFile(cmd.top, true)
        Qcow2 base = vfs.getFile(cmd.base, true)
        Qcow2.commit(vfs, top, base)
        return vfs.getFile(cmd.base, true)
    }

    @Override
    Qcow2 blockPull(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockPullCmd cmd, VolumeInventory volume) {
        String primaryStorageUuid = Q.New(VolumeVO.class).select(VolumeVO_.primaryStorageUuid)
                .eq(VolumeVO_.uuid, volume.uuid).findValue()
        VFS vfs = NfsPrimaryStorageSpec.vfs(primaryStorageUuid, spec)
        Qcow2 volumePath = vfs.getFile(volume.getInstallPath(), true)
        Qcow2.pull(vfs, cmd.base, volumePath)
        return vfs.getFile(volume.getInstallPath(), true)
    }
}
