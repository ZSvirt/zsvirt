package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.HostInventory
import org.zstack.sdk.LocalStorageMigrateVolumeAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.primary.PrimaryStorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2018/3/23.
 */
class LocalStorageVolumeMigrateFailureRollbackCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(PrimaryStorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            testVolumeStatusAndVmStateRollback()
        }
    }

    void testVolumeStatusAndVmStateRollback() {
        HostInventory hostInventory = env.inventoryByName("kvm1")
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        VolumeInventory dataVolume = vm1.getAllVolumes().find { i -> i.getUuid() != vm1.getRootVolumeUuid() }
        detachDataVolumeFromVm {
            uuid = dataVolume.getUuid()
            vmUuid = vm1.getUuid()
        }

        stopVmInstance {
            uuid = vm1.uuid
        }

        env.afterSimulator(LocalStorageKvmBackend.CHECK_MD5_PATH) { rsp ->
            throw new HttpError(403, "on purpose")
        }

        def action = new LocalStorageMigrateVolumeAction()
        action.sessionId = adminSession()
        action.destHostUuid = hostInventory.uuid
        action.volumeUuid = vm1.getRootVolumeUuid()
        LocalStorageMigrateVolumeAction.Result ret = action.call()

        assert ret.error != null

        retryInSecs {
            assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm1.getUuid()).eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()
            assert Q.New(VolumeVO.class).eq(VolumeVO_.uuid, vm1.getRootVolumeUuid()).eq(VolumeVO_.status, VolumeStatus.Ready).isExists()
        }
    }
}
