package org.zstack.test.integration.storage.primary.ceph

import org.zstack.core.db.Q
import org.zstack.core.trash.StorageTrash
import org.zstack.core.trash.TrashType
import org.zstack.header.core.trash.InstallPathRecycleInventory
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/1/8.*/
class ReverCephVolumeWithoutSnapshotCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    VolumeInventory data
    VolumeInventory root
    VolumeSnapshotInventory rootSnapshot
    VolumeSnapshotInventory dataSnapshot
    ImageInventory image1
    VmInstanceInventory vm

    StorageTrash trash


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
        env = CephEnv.CephStorageOneVmWithDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            dataSnapshot = createVolumeSnapshot {
                volumeUuid = data.uuid
                name = "data-snapshot"
            } as VolumeSnapshotInventory

            rootSnapshot = createVolumeSnapshot {
                volumeUuid = root.uuid
                name = "root-snapshot"
            } as VolumeSnapshotInventory

            createDataVolumeFromVolumeSnapshot {
                name = "test-data-vol-from-snap"
                volumeSnapshotUuid = dataSnapshot.uuid
            }

            stopVmInstance {
                uuid = vm.uuid
            }

            testRollbackVolumeFromSnapshot()
            reImage()
            testRollbackVolumeFromRootSnapshotAfterReImage()

            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void prepare() {
        ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        image1 = env.inventoryByName("image1") as ImageInventory
        data = env.inventoryByName("volume") as VolumeInventory
        vm = env.inventoryByName("test-vm") as VmInstanceInventory
        root = queryVolume {
            conditions = ["uuid=${vm.rootVolumeUuid}"]
        }[0] as VolumeInventory

        trash = bean(StorageTrash.class)
    }

    void testRollbackVolumeFromSnapshot() {
        data = queryVolume {
            conditions = ["uuid=${data.uuid}"]
        }[0] as VolumeInventory
        def installPath = data.installPath
        def size = data.size

        def count = Q.New(VolumeSnapshotVO.class).count()
        revertVolumeFromSnapshot {
            uuid = dataSnapshot.uuid
        }

        data = queryVolume {
            conditions = ["uuid=${data.uuid}"]
        }[0] as VolumeInventory

        assert data.installPath == installPath   // installPath not changed
        assert data.size == size

        def trashs = trash.getTrashList(ps.uuid) as List<InstallPathRecycleInventory>
        def trashed = false
        trashs.each { t ->
            if (t.resourceUuid == data.uuid) {
                assert t.installPath == installPath
                assert t.size == size
                assert t.trashType == TrashType.RevertVolume.toString()
                trashed = true
            }
        }
        assert !trashed // installPath not changed, so no trash
        assert Q.New(VolumeSnapshotVO.class).count() == count
    }

    void reImage() {
        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
    }

    void testRollbackVolumeFromRootSnapshotAfterReImage() {
        root = queryVolume {
            conditions = ["uuid=${root.uuid}"]
        }[0] as VolumeInventory

        def installPath = root.installPath
        def size = root.size
        def snapshotInstallPath = rootSnapshot.primaryStorageInstallPath
        assert root.installPath != snapshotInstallPath

        def count = Q.New(VolumeSnapshotVO.class).count()
        revertVolumeFromSnapshot {
            uuid = rootSnapshot.uuid
        }

        def root = queryVolume {
            conditions = ["uuid=${root.uuid}"]
        }[0] as VolumeInventory

        assert root.installPath == snapshotInstallPath.split("@")[0]   // installPath changed
        assert root.size == size

        def trashs = trash.getTrashList(ps.uuid) as List<InstallPathRecycleInventory>
        def trashed = false
        trashs.each { t ->
            if (t.resourceUuid == root.uuid && t.trashType != TrashType.ReimageVolume.toString()) {
                assert t.installPath == installPath
                assert t.size == size
                assert t.trashType == TrashType.RevertVolume.toString()
                trashed = true
            }
        }
        assert trashed
        assert Q.New(VolumeSnapshotVO.class).count() == count
    }
}
