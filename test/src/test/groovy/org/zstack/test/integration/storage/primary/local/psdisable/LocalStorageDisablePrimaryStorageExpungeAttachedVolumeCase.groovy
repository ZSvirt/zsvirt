package org.zstack.test.integration.storage.primary.local.psdisable

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.ImageSpec 
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.DiskOfferingInventory 
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.DetachDataVolumeFromVmAction
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.volume.VolumeGlobalConfig
import org.zstack.header.volume.VolumeDeletionPolicyManager

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageDisablePrimaryStorageExpungeAttachedVolumeCase extends SubCase{
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
            testLocalStorageExpungeAttachedVolumeWhenPrimaryStorageIsDisabled()
        }
    }

    void testLocalStorageExpungeAttachedVolumeWhenPrimaryStorageIsDisabled() {
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Delay.toString())
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("test-vm")

        VolumeInventory dataVolume = createDataVolume {
            name = "dataVolume"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = primaryStorageSpec.inventory.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }

        Map cmd1 = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            cmd1 = json(e.body, LinkedHashMap.class)
            return rsp
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = dataVolume.uuid
        }
        assert cmd1 != null

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled
        

        deleteDataVolume {
            uuid = dataVolume.uuid
        }

        Map cmd2 = null
        env.afterSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH) { rsp, HttpEntity<String> e ->
            cmd2 = json(e.body, LinkedHashMap.class)
            return rsp
        }
        expungeDataVolume {
            uuid = dataVolume.uuid
        }
        assert cmd2 != null


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
