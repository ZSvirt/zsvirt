package org.zstack.testlib.vfs.storage

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeInventory
import org.zstack.kvm.KVMAgentCommands
import org.zstack.testlib.vfs.Qcow2
import org.zstack.testlib.vfs.VFS
import org.zstack.testlib.vfs.extensions.VFSSnapshot
import org.zstack.utils.path.PathUtil

import java.nio.file.Path

trait AbstractFileSystemBasedVFSPrimaryStorageBackend {
    VFSSnapshot doTakeSnapshot(VFS vfs, KVMAgentCommands.TakeSnapshotCmd cmd, VolumeInventory volume) {
        Qcow2 previous = vfs.getFile(volume.installPath, true)
        String newVolumePath = cmd.newVolumeInstallPath != null ? cmd.newVolumeInstallPath :
                vfs.getPath(cmd.installPath).getParent().resolve("${Platform.uuid}.qcow2").toAbsolutePath().toString()

        VFSSnapshot snapshot = new VFSSnapshot()
        if (cmd.fullSnapshot) {
            Qcow2 newBase = previous.copyWithoutBackingFile(cmd.installPath)
            vfs.createQcow2(newVolumePath, 0L, newBase.virtualSize, newBase.pathString())
            snapshot.size = newBase.virtualSize
        } else {
            vfs.createQcow2(newVolumePath, 0L, previous.virtualSize, previous.pathString())
            snapshot.size = previous.virtualSize
        }

        snapshot.installPath = newVolumePath
        return snapshot
    }

    List<TakeSnapshotsOnKvmResultStruct> doTakeSnapshotsOnVolumes(VFS vfs, List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {
        List<TakeSnapshotsOnKvmResultStruct> ret = []

        snapshotJobs.each { job ->
            if (job.memory) {
                String snapshotDir = PathUtil.parentFolder(job.installPath)
                if (!vfs.exists(snapshotDir)) {
                    vfs.createDirectories(snapshotDir)
                }

                VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, job.vmInstanceUuid).find();
                vfs.createRaw(job.installPath, vm.memorySize)
                ret.add(new TakeSnapshotsOnKvmResultStruct(
                        volumeUuid: job.volumeUuid,
                        size: vm.memorySize,
                        previousInstallPath: job.previousInstallPath,
                        installPath: job.installPath
                ))
                return
            }

            Qcow2 previous = vfs.getFile(job.previousInstallPath, true)
            String newVolumePath = job.newVolumeInstallPath != null ? job.newVolumeInstallPath :
                    vfs.getPath(job.installPath).getParent().resolve("${Platform.uuid}.qcow2").toAbsolutePath().toString()

            if (job.full) {
                Qcow2 newBase = previous.copyWithoutBackingFile(job.installPath)
                vfs.createQcow2(newVolumePath, 0L, 0L, newBase.pathString())
                ret.add(new TakeSnapshotsOnKvmResultStruct(
                        volumeUuid: job.volumeUuid,
                        size: 0,
                        previousInstallPath: job.installPath,
                        installPath: newVolumePath
                ))
            } else {
                vfs.createQcow2(newVolumePath, 0L, 0L, previous.pathString())
                ret.add(new TakeSnapshotsOnKvmResultStruct(
                        volumeUuid: job.volumeUuid,
                        size: 0,
                        previousInstallPath: job.previousInstallPath,
                        installPath: newVolumePath
                ))
            }
        }

        return ret
    }

    boolean blockStream(VFS vfs, VolumeInventory volume) {
        vfs.createQcow2(volume.getInstallPath(), 0L, 1L)
    }
}