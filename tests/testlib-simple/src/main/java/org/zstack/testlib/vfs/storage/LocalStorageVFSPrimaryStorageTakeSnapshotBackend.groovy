package org.zstack.testlib.vfs.storage

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.storage.primary.PrimaryStorageVO_
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.volume.VolumeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.storage.primary.local.LocalStorageConstants
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.LocalStorageSpec
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSPrimaryStorageTakeSnapshotBackend
import org.zstack.testlib.vfs.extensions.VFSSnapshot
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class LocalStorageVFSPrimaryStorageTakeSnapshotBackend implements AbstractFileSystemBasedVFSPrimaryStorageBackend, VFSPrimaryStorageTakeSnapshotBackend {
    private static final CLogger logger = Utils.getLogger(LocalStorageVFSPrimaryStorageTakeSnapshotBackend.class)

    @Override
    String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE
    }

    @Override
    VFSSnapshot takeSnapshot(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume) {
        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                .findValue()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)

        if (cmd.isOnline()) {
            vfs.Assert(vfs.exists(cmd.installPath), "cannot find file[${cmd.installPath}]")
            vfs.delete(cmd.installPath)
        }

        VFSSnapshot snapshot = new VFSSnapshot()
        if (cmd.fullSnapshot) {
            Qcow2 vol = vfs.getFile(cmd.volumeInstallPath)
            vol.rebase((String) null)
            Qcow2 newVol = vfs.createQcow2(cmd.installPath, vol.virtualSize, vol.virtualSize, vol.pathString())
            snapshot.installPath = newVol.pathString()
            snapshot.size = vol.virtualSize
        } else {
            Qcow2 vol = vfs.getFile(cmd.volumeInstallPath)
            assert vol: "cannot find volume[${cmd.volumeInstallPath}] on VFS[id: ${vfs.id}]"
            Qcow2 newVol = vfs.createQcow2(cmd.installPath, vol.virtualSize, vol.virtualSize, vol.pathString())
            snapshot.installPath = newVol.pathString()
            snapshot.size = vol.virtualSize
        }

        return snapshot
    }

    @Override
    void mergeSnapshots(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.MergeSnapshotCmd cmd,
                        VolumeInventory volume) {
        VolumeSnapshotVO src = SQL.New("select sp from VolumeSnapshotVO sp, VolumeVO vol where sp.volumeUuid = vol.uuid and" +
                " vol.vmInstanceUuid = :vmUuid and sp.primaryStorageInstallPath = :srcPath" +
                " and vol.deviceId = :deviceId", VolumeSnapshotVO.class)
                .param("vmUuid", cmd.vmUuid)
                .param("srcPath", cmd.srcPath)
                .param("deviceId", cmd.volume.getDeviceId())
                .find()

        assert src || cmd.fullRebase: "cannot find source snapshot[path: ${cmd.srcPath}] of VM[uuid: ${cmd.vmUuid}] in database"
        VolumeSnapshotInventory sp = src?.toInventory()

        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                .findValue()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)

        vfs.Assert(vfs.exists(volume.installPath), "missing snapshot install ${volume.installPath}")
        Qcow2 to = vfs.getFile(volume.installPath, true)
        assert to.backingFile != null : "the target[${to.pathString()}] volume has no backing file"

        if (cmd.fullRebase) {
            to.rebase((String)null)
        } else {
            vfs.Assert(vfs.exists(sp.primaryStorageInstallPath), "missing snapshot install ${sp.primaryStorageInstallPath}")
            Qcow2 from = vfs.getFile(sp.primaryStorageInstallPath, true)
            assert to.backingQcow2().pathString() != from.pathString() : "the snapshot is volume's backing file, this must be a fullRebase: ${e.body}"
            to.rebase(from.path)
        }
    }

    @Override
    List<TakeSnapshotsOnKvmResultStruct> takeSnapshotsOnVolumes(String primaryStorageUuid, HttpEntity<String> e, EnvSpec spec, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        TakeSnapshotsOnKvmJobStruct job0 = snapshotJobs[0]
        String storagePath = SQL.New("select pri.mountPath from PrimaryStorageVO pri, VolumeVO vol where" +
                " pri.uuid = vol.primaryStorageUuid and vol.uuid = :uuid", String.class).param("uuid", job0.volumeUuid)
                .find()

        assert storagePath : "cannot find primary storage of snapshot[uuid: ${job0.snapshotUuid}]"

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        doTakeSnapshotsOnVolumes(vfs, snapshotJobs)
    }

    @Override
    void blockStream(HttpEntity<String> e, EnvSpec spec, VolumeInventory volume) {
        String storagePath = SQL.New("select pri.mountPath from PrimaryStorageVO pri, VolumeVO vol where" +
                " pri.uuid = vol.primaryStorageUuid and vol.uuid = :uuid", String.class).param("uuid", volume.getUuid())
                .find()

        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        if (vfs.exists(volume.getInstallPath())) {
            vfs.delete(volume.getInstallPath())
        }
        blockStream(vfs, volume)
    }

    @Override
    Qcow2 blockCommit(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockCommitCmd cmd, VolumeInventory volume) {
        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                .findValue()
        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        Qcow2 top = vfs.getFile(cmd.top, true)
        Qcow2 base = vfs.getFile(cmd.base, true)
        Qcow2.commit(vfs, top, base)
        return vfs.getFile(cmd.base, true)
    }

    @Override
    Qcow2 blockPull(HttpEntity<String> e, EnvSpec spec, KVMAgentCommands.BlockPullCmd cmd, VolumeInventory volume) {
        String storagePath = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.mountPath)
                .eq(PrimaryStorageVO_.uuid, volume.primaryStorageUuid)
                .findValue()
        VFS vfs = LocalStorageSpec.vfs(LocalStorageSpec.hostUuidFromHTTPHeaders(e), storagePath, spec)
        Qcow2 volumePath = vfs.getFile(volume.getInstallPath(), true)
        Qcow2.pull(vfs, cmd.base, volumePath)
        return vfs.getFile(volume.getInstallPath(), true)
    }
}
