package org.zstack.test.integration.storage.primary.local.psmaintenance

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

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageMaintenancePrimaryStorageDetachVolumeCase extends SubCase{
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
            testLocalStorageDetachVolumeWhenPrimaryStorageIsMaintained()
        }
    }


    void testLocalStorageDetachVolumeWhenPrimaryStorageIsMaintained() {
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

        def attach_volume_path_is_invoked = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            //cmd1 = json(e.body, LinkedHashMap.class)
            attach_volume_path_is_invoked = true
            return rsp
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = dataVolume.uuid
        }
        assert attach_volume_path_is_invoked


        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Maintenance


        //Map cmd2 = null
        def detach_volume_path_is_invoked = false
        env.afterSimulator(KVMConstant.KVM_DETACH_VOLUME) { rsp, HttpEntity<String> e ->
            //cmd2 = json(e.body, LinkedHashMap.class)
            detach_volume_path_is_invoked = true
            return rsp
        }

        DetachDataVolumeFromVmAction a = new DetachDataVolumeFromVmAction()
        a.uuid = dataVolume.uuid
        a.sessionId = currentEnvSpec.session.uuid

        DetachDataVolumeFromVmAction.Result res = a.call()
        assert !detach_volume_path_is_invoked


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
