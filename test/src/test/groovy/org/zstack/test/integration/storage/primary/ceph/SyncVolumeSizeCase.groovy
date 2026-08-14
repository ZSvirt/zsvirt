package org.zstack.test.integration.storage.primary.ceph

import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.storage.volume.VolumeSystemTags
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2019/5/23.
 */
class SyncVolumeSizeCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testSyncVolumeSize()
        }
    }

    void testSyncVolumeSize(){
        VmInstanceInventory vm = env.inventoryByName("test-vm") as VmInstanceInventory

        long size = 1
        long actualSize = 10

        env.hijackSimulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { CephPrimaryStorageBase.GetVolumeSizeRsp rsp ->
            rsp.size = size
            rsp.actualSize = actualSize
            return rsp
        }

        VolumeInventory volume = syncVolumeSize {
            uuid = vm.rootVolumeUuid
        }
        assert volume.actualSize == actualSize
        assert !VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.hasTag(vm.rootVolumeUuid)

        env.hijackSimulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { CephPrimaryStorageBase.GetVolumeSizeRsp rsp ->
            rsp.size = size
            rsp.actualSize = null
            return rsp
        }

        syncVolumeSize {
            uuid = vm.rootVolumeUuid
        }

        assert VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.hasTag(vm.rootVolumeUuid)


        env.hijackSimulator(CephPrimaryStorageBase.GET_VOLUME_SIZE_PATH) { CephPrimaryStorageBase.GetVolumeSizeRsp rsp ->
            rsp.size = size
            rsp.actualSize = actualSize
            return rsp
        }

        syncVolumeSize {
            uuid = vm.rootVolumeUuid
        }

        assert !VolumeSystemTags.NOT_SUPPORT_ACTUAL_SIZE_FLAG.hasTag(vm.rootVolumeUuid)

        env.cleanFinalSimulatorHandlers()
    }
    
    @Override
    void clean() {
        env.delete()
    }
}
