package org.zstack.test.integration.storage.primary.smp

import org.zstack.core.Platform
import org.zstack.sdk.*
import org.zstack.storage.primary.PrimaryStorageDeleteBitGC
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.primary.smp.KvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.test.integration.storage.volume.VolumeEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil

/**
 * Created by mingjian.deng on 2018/1/22.
 */
class CreateSmpRootVolumeFromVolumeSnapshotCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps
    KVMHostInventory kvm
    ImageInventory image
    VmInstanceInventory vm
    SftpBackupStorageInventory sftp

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
        env = VolumeEnv.smpStorageSftpEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateRootVolumeTemplate()
        }
    }

    void prepare() {
        ps = env.inventoryByName("smp-ps") as PrimaryStorageInventory
        kvm = env.inventoryByName("kvm") as KVMHostInventory
        image = env.inventoryByName("image-data-volume") as ImageInventory
        sftp = env.inventoryByName("sftp") as SftpBackupStorageInventory
        vm = env.inventoryByName("test-vm")
    }

    void testCreateRootVolumeTemplate() {
        def rootSnap = createVolumeSnapshot {
            name = "test-root-snap"
            volumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotInventory

        createRootVolumeTemplateFromVolumeSnapshot {
            name = "test-root-volume-template"
            snapshotUuid = rootSnap.uuid
            backupStorageUuids = [sftp.uuid]
        }
    }
}
