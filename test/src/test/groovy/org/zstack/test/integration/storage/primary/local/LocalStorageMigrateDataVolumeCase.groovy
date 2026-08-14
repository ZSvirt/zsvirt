package org.zstack.test.integration.storage.primary.local

import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HostSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch

class LocalStorageMigrateDataVolumeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageMigrateDataVolume()
        }
    }

    void testLocalStorageMigrateDataVolume() {
        HostSpec hostSpec1 = (HostSpec) env.specByName("kvm1")
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        VolumeInventory dataVolume = vm1.getAllVolumes().find { i -> i.getUuid() != vm1.getRootVolumeUuid() }
        detachDataVolumeFromVm {
            uuid = dataVolume.getUuid()
            vmUuid = vm1.getUuid()
        }

        boolean calledCheckMD5 = false
        env.afterSimulator(LocalStorageKvmBackend.CHECK_MD5_PATH) { rsp ->
            calledCheckMD5 = true
            return rsp
        }

        boolean calledCopyToRemote = false
        env.preSimulator(LocalStorageKvmMigrateVmFlow.COPY_TO_REMOTE_BITS_PATH) {
            calledCopyToRemote = true
        }

        CountDownLatch latch = new CountDownLatch(1)

        // pretend that taking snapshot is slow (1.5 second)
        env.preSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) {
            latch.countDown()
            Thread.sleep(1000)
        }

        VolumeSnapshotInventory inv = null
        Thread.start {
            inv = createVolumeSnapshot {
                volumeUuid = dataVolume.getUuid()
                name = "volume-snapshot"
            }
        }

        // wait snapshot message being handled
        latch.await()
        localStorageMigrateVolume {
            volumeUuid = dataVolume.getUuid()
            destHostUuid = hostSpec1.inventory.uuid
        }

        retryInSecs(6) {
            assert calledCheckMD5 && calledCopyToRemote
        }

        deleteVolumeSnapshot {
            uuid = inv.uuid
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
