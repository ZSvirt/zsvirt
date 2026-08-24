package org.zstack.test.integration.storage.primary.nfs

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackend
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by AlanJager on 2017/3/25.
 */
class NfsDiskCapacityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testAvailableCapacityDuringDataVolumeLifecycle()
            testDeleteSnapshotChainRestoresPrimaryStorageCapacity()
            testUndoSnapshotCreationRestoresPrimaryStorageCapacity()
            testCommitDeleteIntermediateSnapshotReturnsDeletedLayerCapacity()
            testPullDeleteIntermediateSnapshotKeepsCapacityConsistent()
        }
    }

    void testAvailableCapacityDuringDataVolumeLifecycle() {
        PrimaryStorageOverProvisioningManager psRatioMgr = bean(PrimaryStorageOverProvisioningManager.class)
        PrimaryStorageInventory primaryStorageInventory = env.inventoryByName("nfs")
        BackupStorageInventory backupStorageInventory = env.inventoryByName("sftp")
        L3NetworkInventory l3NetworkInventory = env.inventoryByName("l3")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")

        psRatioMgr.setGlobalRatio(1)

        env.afterSimulator(SftpBackupStorageConstant.DOWNLOAD_IMAGE_PATH) { rsp ->
            rsp.size = SizeUnit.GIGABYTE.toByte(10)
            rsp.actualSize = SizeUnit.GIGABYTE.toByte(1)
            return rsp
        }

        ImageInventory imageInventory = addImage {
            name = "test-image"
            resourceUuid = Platform.getUuid()
            backupStorageUuids = [backupStorageInventory.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
            url = "http://image.qcow2"
        }

        assert imageInventory.size == SizeUnit.GIGABYTE.toByte(10)
        assert imageInventory.actualSize == SizeUnit.GIGABYTE.toByte(1)

        VmInstanceInventory vmInstanceInventory = createVmInstance {
            name = "test-vm"
            cpuNum = 4
            memorySize = gb(8)
            imageUuid = imageInventory.uuid
            l3NetworkUuids = [l3NetworkInventory.uuid]
        }

        GetPrimaryStorageCapacityResult beforePrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        GetPrimaryStorageCapacityResult getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        psRatioMgr.setGlobalRatio(2.5)

        GetPrimaryStorageCapacityResult ratio251 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        assert ratio251.availableCapacity == getPrimaryStorageCapacityResult.availableCapacity

        VolumeInventory volumeInventory1 = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volumeInventory1.uuid
            vmInstanceUuid = vmInstanceInventory.uuid
        }

        getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        psRatioMgr.setGlobalRatio(1.5)
        GetPrimaryStorageCapacityResult ratio15 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }
        assert getPrimaryStorageCapacityResult.availableCapacity == ratio15.availableCapacity


        VolumeInventory volumeInventory2 = createDataVolume {
            name = "data-volume"
            diskOfferingUuid = diskOfferingInventory.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volumeInventory2.uuid
            vmInstanceUuid = vmInstanceInventory.uuid
        }
        deleteDataVolume {
            uuid = volumeInventory1.uuid
        }
        expungeDataVolume {
            uuid = volumeInventory1.uuid
        }

        retryInSecs {
            psRatioMgr.setGlobalRatio(1.5)
            getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            psRatioMgr.setGlobalRatio(2.5)
            GetPrimaryStorageCapacityResult ratio252 = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            assert getPrimaryStorageCapacityResult.availableCapacity == ratio252.availableCapacity
        }

        deleteDataVolume {
            uuid = volumeInventory2.uuid
        }
        expungeDataVolume {
            uuid = volumeInventory2.uuid
        }

        retryInSecs {
            psRatioMgr.setGlobalRatio(2.5)
            getPrimaryStorageCapacityResult = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            psRatioMgr.setGlobalRatio(1.0)
            GetPrimaryStorageCapacityResult ratio1 = getPrimaryStorageCapacity {
                primaryStorageUuids = [primaryStorageInventory.uuid]
            }
            assert getPrimaryStorageCapacityResult.availableCapacity == ratio1.availableCapacity
        }



        GetPrimaryStorageCapacityResult getPrimaryStorageCapacityResult1 = getPrimaryStorageCapacity {
            primaryStorageUuids = [primaryStorageInventory.uuid]
        }

        assert beforePrimaryStorageCapacityResult.totalCapacity == getPrimaryStorageCapacityResult1.totalPhysicalCapacity
        assert beforePrimaryStorageCapacityResult.availableCapacity == getPrimaryStorageCapacityResult1.availableCapacity
    }

    void testUndoSnapshotCreationRestoresPrimaryStorageCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VolumeInventory volume = createDataVolume {
            name = "nfs-capacity-accounting-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        long snapshotSize = SizeUnit.GIGABYTE.toByte(20)
        long activeLayerSize = SizeUnit.GIGABYTE.toByte(5)
        String originalInstallPath = volume.installPath
        String activeLayerInstallPath = null
        AtomicInteger activeLayerSizeQueries = new AtomicInteger()

        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSize
                return rsp
            }
            env.hijackSimulator(NfsPrimaryStorageKVMBackend.GET_VOLUME_SIZE_PATH) { NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeRsp rsp, HttpEntity<String> e ->
                def cmd = json(e.body, NfsPrimaryStorageKVMBackendCommands.GetVolumeActualSizeCmd.class)
                rsp.actualSize = cmd.installPath == originalInstallPath ? snapshotSize : activeLayerSize
                rsp.size = volume.size
                if (activeLayerInstallPath != null && cmd.installPath == activeLayerInstallPath) {
                    activeLayerSizeQueries.incrementAndGet()
                }
                return rsp
            }
            env.hijackSimulator(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_COMMIT) { NfsPrimaryStorageKVMBackendCommands.OfflineCommitSnapshotRsp rsp, HttpEntity<String> e ->
                rsp.actualSize = snapshotSize
                return rsp
            }

            long availableBeforeSnapshot = availableCapacity(ps.uuid)

            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-capacity-accounting-snapshot"
            } as VolumeSnapshotInventory

            activeLayerInstallPath = Q.New(VolumeVO.class)
                    .eq(VolumeVO_.uuid, volume.uuid)
                    .select(VolumeVO_.installPath)
                    .findValue()
            assert activeLayerInstallPath != originalInstallPath

            long snapshotSizeInDb = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.uuid, snapshot.uuid)
                    .select(VolumeSnapshotVO_.size)
                    .findValue()
            long availableAfterSnapshot = availableCapacity(ps.uuid)
            assert snapshotSizeInDb == snapshotSize
            assert availableAfterSnapshot == availableBeforeSnapshot - snapshotSize

            undoSnapshotCreation {
                uuid = volume.uuid
                snapShotUuid = snapshot.uuid
            }

            assert activeLayerSizeQueries.get() > 0 :
                    "undo snapshot creation must query the NFS active layer: installPath=${activeLayerInstallPath}"
            assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).isExists()
            VolumeVO volumeAfterUndo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).find()
            assert volumeAfterUndo.installPath == originalInstallPath
            assert volumeAfterUndo.actualSize == snapshotSize

            long availableAfterUndo = availableCapacity(ps.uuid)
            assert availableAfterUndo == availableBeforeSnapshot :
                    "undo snapshot creation must restore NFS capacity: expected=${availableBeforeSnapshot}, actual=${availableAfterUndo}, deficit=${availableBeforeSnapshot - availableAfterUndo}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testCommitDeleteIntermediateSnapshotReturnsDeletedLayerCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VolumeInventory volume = createDataVolume {
            name = "nfs-intermediate-snapshot-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        long parentSnapshotSize = SizeUnit.GIGABYTE.toByte(20)
        long childSnapshotSize = SizeUnit.GIGABYTE.toByte(5)
        long mergedSnapshotSize = parentSnapshotSize
        AtomicInteger snapshotSequence = new AtomicInteger()

        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSequence.getAndIncrement() == 0 ? parentSnapshotSize : childSnapshotSize
                return rsp
            }
            env.hijackSimulator(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_COMMIT) { NfsPrimaryStorageKVMBackendCommands.OfflineCommitSnapshotRsp rsp, HttpEntity<String> e ->
                rsp.actualSize = mergedSnapshotSize
                return rsp
            }

            long availableBeforeSnapshots = availableCapacity(ps.uuid)

            VolumeSnapshotInventory parentSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-intermediate-parent-snapshot"
            } as VolumeSnapshotInventory
            VolumeSnapshotInventory childSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-intermediate-child-snapshot"
            } as VolumeSnapshotInventory

            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == parentSnapshotSize
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == childSnapshotSize
            assert availableCapacity(ps.uuid) == availableBeforeSnapshots - parentSnapshotSize - childSnapshotSize

            deleteVolumeSnapshot {
                uuid = parentSnapshot.uuid
                scope = "single"
                direction = "commit"
            }

            assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid).isExists()
            long retainedSnapshotSize = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size)
                    .findValue()
            assert retainedSnapshotSize == mergedSnapshotSize
            String retainedSnapshotParentUuid = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.parentUuid)
                    .findValue()
            assert retainedSnapshotParentUuid == parentSnapshot.parentUuid

            long availableAfterCommitDelete = availableCapacity(ps.uuid)
            assert availableAfterCommitDelete == availableBeforeSnapshots - mergedSnapshotSize :
                    "commit deletion must return only the deleted child layer capacity: " +
                            "expected=${availableBeforeSnapshots - mergedSnapshotSize}, actual=${availableAfterCommitDelete}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testDeleteSnapshotChainRestoresPrimaryStorageCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VolumeInventory volume = createDataVolume {
            name = "nfs-chain-delete-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        long snapshotSize = SizeUnit.GIGABYTE.toByte(20)
        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSize
                return rsp
            }

            long availableBeforeSnapshot = availableCapacity(ps.uuid)
            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-chain-delete-snapshot"
            } as VolumeSnapshotInventory

            assert Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.uuid, snapshot.uuid)
                    .select(VolumeSnapshotVO_.size)
                    .findValue() == snapshotSize
            assert availableCapacity(ps.uuid) == availableBeforeSnapshot - snapshotSize

            deleteVolumeSnapshot {
                uuid = snapshot.uuid
            }

            assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).isExists()
            assert availableCapacity(ps.uuid) == availableBeforeSnapshot
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testPullDeleteIntermediateSnapshotKeepsCapacityConsistent() {
        PrimaryStorageInventory ps = env.inventoryByName("nfs") as PrimaryStorageInventory
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VolumeInventory volume = createDataVolume {
            name = "nfs-pull-snapshot-volume"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        long parentSnapshotSize = SizeUnit.GIGABYTE.toByte(20)
        long childSnapshotSize = SizeUnit.GIGABYTE.toByte(5)
        long mergedSnapshotSize = parentSnapshotSize
        AtomicInteger snapshotSequence = new AtomicInteger()

        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSequence.getAndIncrement() == 0 ? parentSnapshotSize : childSnapshotSize
                return rsp
            }
            env.hijackSimulator(NfsPrimaryStorageKVMBackend.OFFLINE_SNAPSHOT_MERGE) { NfsPrimaryStorageKVMBackendCommands.OfflineMergeSnapshotRsp rsp, HttpEntity<String> e ->
                rsp.actualSize = mergedSnapshotSize
                return rsp
            }

            long availableBeforeSnapshots = availableCapacity(ps.uuid)

            VolumeSnapshotInventory parentSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-pull-parent-snapshot"
            } as VolumeSnapshotInventory
            VolumeSnapshotInventory childSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "nfs-pull-child-snapshot"
            } as VolumeSnapshotInventory

            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == parentSnapshotSize
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == childSnapshotSize
            assert availableCapacity(ps.uuid) == availableBeforeSnapshots - parentSnapshotSize - childSnapshotSize

            deleteVolumeSnapshot {
                uuid = parentSnapshot.uuid
                scope = "single"
                direction = "pull"
            }

            assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid).isExists()
            VolumeSnapshotVO retainedSnapshot = Q.New(VolumeSnapshotVO.class)
                    .eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .find()
            assert retainedSnapshot.size == mergedSnapshotSize
            assert retainedSnapshot.parentUuid == parentSnapshot.parentUuid

            long availableAfterPullDelete = availableCapacity(ps.uuid)
            assert availableAfterPullDelete == availableBeforeSnapshots - mergedSnapshotSize :
                    "pull deletion must preserve only the merged child layer capacity: " +
                            "expected=${availableBeforeSnapshots - mergedSnapshotSize}, actual=${availableAfterPullDelete}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    private static long availableCapacity(String primaryStorageUuid) {
        return Q.New(PrimaryStorageCapacityVO.class)
                .eq(PrimaryStorageCapacityVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .findValue()
    }
}
