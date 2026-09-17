package org.zstack.test.integration.storage.primary.local.psmaintenance

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.header.volume.VolumeDeletionPolicyManager

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageMaintenancePrimaryStorageCreateVolumeCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnvForPrimaryStorage()
    }

    @Override
    void test() {
        env.create {
            testLocalStorageCreateVolumeWhenPrimaryStorageIsMaintained()
        }
    }


    void testLocalStorageCreateVolumeWhenPrimaryStorageIsMaintained() {
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Delay.toString())
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Maintenance


        def create_empty_volume_path_is_invoked = false
        env.afterSimulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) { rsp, HttpEntity<String> e ->
            //cmd1 = json(e.body, LocalStorageKvmBackend.CreateEmptyVolumeCmd.class)
            create_empty_volume_path_is_invoked = true
            return rsp
        }
        VolumeInventory dataVolume = createDataVolume {
            name = "dataVolume"
            diskSize = gb(20)
            //primaryStorageUuid = primaryStorageSpec.inventory.uuid
            //systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        assert !create_empty_volume_path_is_invoked 


        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
