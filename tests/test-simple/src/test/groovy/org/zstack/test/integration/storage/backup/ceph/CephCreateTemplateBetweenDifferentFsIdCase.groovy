package org.zstack.test.integration.storage.backup.ceph

import org.zstack.core.db.Q
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by mingjian.deng on 2018/1/3.
 */
class CephCreateTemplateBetweenDifferentFsIdCase extends SubCase {
    EnvSpec env
    VolumeInventory volume
    VmInstanceInventory vm
    PrimaryStorageInventory ps
    BackupStorageInventory bs
    BackupStorageInventory bs1

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
        env = CephEnv.DifferentFsIdCephStorageOneVmWithDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateRootVolumeTemplateFailed()
        }
    }

    void prepare() {
        volume = env.inventoryByName("volume") as VolumeInventory
        vm = env.inventoryByName("test-vm") as VmInstanceInventory
        ps = env.inventoryByName("ceph-pri") as PrimaryStorageInventory
        bs = env.inventoryByName("ceph-bk") as BackupStorageInventory
        bs1 = env.inventoryByName("ceph-bk-1") as BackupStorageInventory
    }

    void testCreateRootVolumeTemplateFailed() {
        def count = Q.New(ImageVO.class).count()
        def snapshot = Q.New(VolumeSnapshotVO.class).count()
        expect([AssertionError.class]) {
            createRootVolumeTemplateFromRootVolume {
                name = "rootVolume"
                rootVolumeUuid = vm.rootVolumeUuid
                backupStorageUuids = [bs1.uuid]
            }
        }
        // make sure no garbage
        assert count == Q.New(ImageVO.class).count()
        assert snapshot == Q.New(VolumeSnapshotVO.class).count()

        def rootVolumeSnapshot = createVolumeSnapshot {
            volumeUuid = vm.rootVolumeUuid
            name = "root-volume-snapshot"
        } as VolumeSnapshotInventory

        snapshot = Q.New(VolumeSnapshotVO.class).count()

        expect([AssertionError.class]) {
            createRootVolumeTemplateFromVolumeSnapshot {
                name = "rootVolumeSnapshot"
                snapshotUuid = rootVolumeSnapshot.uuid
                platform = ImagePlatform.Linux.name()
                backupStorageUuids = [bs1.uuid]
            }
        }
        // make sure no garbage
        assert count == Q.New(ImageVO.class).count()
        assert snapshot == Q.New(VolumeSnapshotVO.class).count()
    }
}
