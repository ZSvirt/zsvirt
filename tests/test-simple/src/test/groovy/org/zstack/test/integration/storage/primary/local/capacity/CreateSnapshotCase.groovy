package org.zstack.test.integration.storage.primary.local.capacity

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by lining on 2017/4/21.
 */

/*
 * step 1 : record current ps available capacity
 * step 2 : create snapshot
 * step 3 : check ps available capacity
 * step 4 : reconnect ps,check ps available capacity
 * step 5 : delete snapshot ,check ps available capacity
 * step 6 : reconnect ps,check ps available capacity
 */
class CreateSnapshotCase extends SubCase {
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
        env = makeEnv {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = ImageConstant.ISO_FORMAT_STRING.toString()
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                cpu = 4
                memoryGB(8)
                useImage("image1")
                useL3Networks("pubL3")
                useHost("kvm")
                disk {
                    sizeGB(20)
                }
                disk {
                    sizeGB(20)
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkPSAvailableCapacityAfterCreateSnapshot()
            testUndoSnapshotCreationRestoresPrimaryStorageCapacity()
            testCommitDeleteIntermediateSnapshotReturnsDeletedLayerCapacity()
            testPullDeleteIntermediateSnapshotKeepsCapacityConsistent()
            testDeleteRootSnapshotWhenVmDestroyed()
        }
    }

    void checkPSAvailableCapacityAfterCreateSnapshot(){
        PrimaryStorageInventory ps = env.inventoryByName("local")
        VmInstanceInventory vm = env.inventoryByName("vm")
        LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()

        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            volumeUuid = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid }.uuid
            name = "sp1"
        }
        assert Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshot.uuid)
                .select(VolumeSnapshotVO_.size)
                .findValue() == snapshot.size

        LocalStorageHostRefVO currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        PrimaryStorageInventory currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size


        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]

        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity + snapshot.size
        assert refVO.availableCapacity  == currentRefVO.availableCapacity + snapshot.size

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
        assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).isExists()
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        currentPs = queryPrimaryStorage {
            conditions=["uuid=${ps.uuid}".toString()]
        }[0]
        currentRefVO = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, vm.hostUuid).find()
        assert ps.availableCapacity == currentPs.availableCapacity
        assert refVO.availableCapacity  == currentRefVO.availableCapacity
    }

    void testUndoSnapshotCreationRestoresPrimaryStorageCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("local") as PrimaryStorageInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        VolumeInventory volume = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid } as VolumeInventory

        long snapshotSize = SizeUnit.GIGABYTE.toByte(20)
        long activeLayerSize = SizeUnit.GIGABYTE.toByte(5)
        String originalInstallPath = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volume.uuid)
                .select(VolumeVO_.installPath)
                .findValue()
        String activeLayerInstallPath = null
        AtomicInteger activeLayerSizeQueries = new AtomicInteger()

        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSize
                return rsp
            }
            env.hijackSimulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) { LocalStorageKvmBackend.GetVolumeSizeRsp rsp, HttpEntity<String> e ->
                def cmd = json(e.body, LocalStorageKvmBackend.GetVolumeSizeCmd.class)
                rsp.actualSize = cmd.installPath == originalInstallPath ? snapshotSize : activeLayerSize
                rsp.size = volume.size
                if (activeLayerInstallPath != null && cmd.installPath == activeLayerInstallPath) {
                    activeLayerSizeQueries.incrementAndGet()
                }
                return rsp
            }
            env.hijackSimulator(KVMConstant.KVM_MERGE_SNAPSHOT_PATH) { KVMAgentCommands.MergeSnapshotRsp rsp, HttpEntity<String> e ->
                rsp.size = snapshotSize
                return rsp
            }

            long globalBeforeSnapshot = availableCapacity(ps.uuid)
            long hostBeforeSnapshot = hostAvailableCapacity(ps.uuid, vm.hostUuid)

            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "local-capacity-accounting-snapshot"
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
            long globalAfterSnapshot = availableCapacity(ps.uuid)
            long hostAfterSnapshot = hostAvailableCapacity(ps.uuid, vm.hostUuid)
            assert snapshotSizeInDb == snapshotSize
            assert globalAfterSnapshot == globalBeforeSnapshot - snapshotSize
            assert hostAfterSnapshot == hostBeforeSnapshot - snapshotSize

            undoSnapshotCreation {
                uuid = volume.uuid
                snapShotUuid = snapshot.uuid
            }

            assert activeLayerSizeQueries.get() > 0 :
                    "undo snapshot creation must query the LocalStorage active layer: installPath=${activeLayerInstallPath}"
            assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).isExists()
            VolumeVO volumeAfterUndo = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volume.uuid).find()
            assert volumeAfterUndo.installPath == originalInstallPath
            assert volumeAfterUndo.actualSize == snapshotSize

            long globalAfterUndo = availableCapacity(ps.uuid)
            long hostAfterUndo = hostAvailableCapacity(ps.uuid, vm.hostUuid)
            assert [globalAfterUndo, hostAfterUndo] == [globalBeforeSnapshot, hostBeforeSnapshot] :
                    "undo snapshot creation must restore LocalStorage capacity: " +
                            "globalExpected=${globalBeforeSnapshot}, globalActual=${globalAfterUndo}, globalDeficit=${globalBeforeSnapshot - globalAfterUndo}, " +
                            "hostExpected=${hostBeforeSnapshot}, hostActual=${hostAfterUndo}, hostDeficit=${hostBeforeSnapshot - hostAfterUndo}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testCommitDeleteIntermediateSnapshotReturnsDeletedLayerCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("local") as PrimaryStorageInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        VolumeInventory volume = vm.allVolumes.find { it.uuid != vm.rootVolumeUuid } as VolumeInventory

        long parentSnapshotSize = SizeUnit.GIGABYTE.toByte(20)
        long childSnapshotSize = SizeUnit.GIGABYTE.toByte(5)
        long mergedSnapshotSize = parentSnapshotSize
        AtomicInteger snapshotSequence = new AtomicInteger()

        try {
            env.hijackSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { KVMAgentCommands.TakeSnapshotResponse rsp, HttpEntity<String> e ->
                rsp.size = snapshotSequence.getAndIncrement() == 0 ? parentSnapshotSize : childSnapshotSize
                return rsp
            }
            env.hijackSimulator(KVMConstant.KVM_BLOCK_COMMIT_VOLUME_PATH) { KVMAgentCommands.BlockCommitResponse rsp, HttpEntity<String> e ->
                rsp.size = mergedSnapshotSize
                return rsp
            }

            long globalBeforeSnapshots = availableCapacity(ps.uuid)
            long hostBeforeSnapshots = hostAvailableCapacity(ps.uuid, vm.hostUuid)

            VolumeSnapshotInventory parentSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "local-intermediate-parent-snapshot"
            } as VolumeSnapshotInventory
            VolumeSnapshotInventory childSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "local-intermediate-child-snapshot"
            } as VolumeSnapshotInventory

            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == parentSnapshotSize
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == childSnapshotSize
            assert availableCapacity(ps.uuid) == globalBeforeSnapshots - parentSnapshotSize - childSnapshotSize
            assert hostAvailableCapacity(ps.uuid, vm.hostUuid) == hostBeforeSnapshots - parentSnapshotSize - childSnapshotSize

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

            long globalAfterCommitDelete = availableCapacity(ps.uuid)
            long hostAfterCommitDelete = hostAvailableCapacity(ps.uuid, vm.hostUuid)
            assert [globalAfterCommitDelete, hostAfterCommitDelete] ==
                    [globalBeforeSnapshots - mergedSnapshotSize, hostBeforeSnapshots - mergedSnapshotSize] :
                    "commit deletion must return only the deleted child layer capacity: " +
                            "globalExpected=${globalBeforeSnapshots - mergedSnapshotSize}, globalActual=${globalAfterCommitDelete}, " +
                            "hostExpected=${hostBeforeSnapshots - mergedSnapshotSize}, hostActual=${hostAfterCommitDelete}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testPullDeleteIntermediateSnapshotKeepsCapacityConsistent() {
        PrimaryStorageInventory ps = env.inventoryByName("local") as PrimaryStorageInventory
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        VolumeInventory volume = createDataVolume {
            name = "local-pull-snapshot-volume"
            diskSize = SizeUnit.GIGABYTE.toByte(20)
            primaryStorageUuid = ps.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
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
            env.hijackSimulator(LocalStorageKvmBackend.OFFLINE_MERGE_PATH) { LocalStorageKvmBackend.OfflineMergeSnapshotRsp rsp, HttpEntity<String> e ->
                rsp.actualSize = mergedSnapshotSize
                return rsp
            }

            long globalBeforeSnapshots = availableCapacity(ps.uuid)
            long hostBeforeSnapshots = hostAvailableCapacity(ps.uuid, host.uuid)

            VolumeSnapshotInventory parentSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "local-pull-parent-snapshot"
            } as VolumeSnapshotInventory
            VolumeSnapshotInventory childSnapshot = createVolumeSnapshot {
                volumeUuid = volume.uuid
                name = "local-pull-child-snapshot"
            } as VolumeSnapshotInventory

            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, parentSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == parentSnapshotSize
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, childSnapshot.uuid)
                    .select(VolumeSnapshotVO_.size).findValue() == childSnapshotSize
            assert availableCapacity(ps.uuid) == globalBeforeSnapshots - parentSnapshotSize - childSnapshotSize
            assert hostAvailableCapacity(ps.uuid, host.uuid) == hostBeforeSnapshots - parentSnapshotSize - childSnapshotSize

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

            long globalAfterPullDelete = availableCapacity(ps.uuid)
            long hostAfterPullDelete = hostAvailableCapacity(ps.uuid, host.uuid)
            assert [globalAfterPullDelete, hostAfterPullDelete] ==
                    [globalBeforeSnapshots - mergedSnapshotSize, hostBeforeSnapshots - mergedSnapshotSize] :
                    "pull deletion must preserve only the merged child layer capacity: " +
                            "globalExpected=${globalBeforeSnapshots - mergedSnapshotSize}, globalActual=${globalAfterPullDelete}, " +
                            "hostExpected=${hostBeforeSnapshots - mergedSnapshotSize}, hostActual=${hostAfterPullDelete}"
        } finally {
            env.cleanFinalSimulatorHandlers()
        }
    }

    void testDeleteRootSnapshotWhenVmDestroyed() {
        PrimaryStorageInventory ps = env.inventoryByName("local") as PrimaryStorageInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        def snapshot = createVolumeSnapshot {
            name = "root-volume-snapshot2"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory
        long snapshotSize = Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshot.uuid)
                .select(VolumeSnapshotVO_.size)
                .findValue()

        stopVmInstance {
            uuid = vm.uuid
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        long globalBeforeDelete = availableCapacity(ps.uuid)
        long hostBeforeDelete = hostAvailableCapacity(ps.uuid, vm.hostUuid)
        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }
        assert !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshot.uuid).isExists()
        assert availableCapacity(ps.uuid) == globalBeforeDelete + snapshotSize
        assert hostAvailableCapacity(ps.uuid, vm.hostUuid) == hostBeforeDelete + snapshotSize
    }

    private static long availableCapacity(String primaryStorageUuid) {
        return Q.New(PrimaryStorageCapacityVO.class)
                .eq(PrimaryStorageCapacityVO_.uuid, primaryStorageUuid)
                .select(PrimaryStorageCapacityVO_.availableCapacity)
                .findValue()
    }

    private static long hostAvailableCapacity(String primaryStorageUuid, String hostUuid) {
        return Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue()
    }
}
