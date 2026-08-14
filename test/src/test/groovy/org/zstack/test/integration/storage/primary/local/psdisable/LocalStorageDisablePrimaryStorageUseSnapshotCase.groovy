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
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.DiskOfferingInventory 
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.DetachDataVolumeFromVmAction
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.primary.local.LocalStorageKvmBackend

/**
 * Created by shengyan on 2017/3/22.
 */
class LocalStorageDisablePrimaryStorageUseSnapshotCase extends SubCase{
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
            testLocalStorageUseSnapshotWhenPrimaryStorageIsDisabled()
        }
    }


    void testLocalStorageUseSnapshotWhenPrimaryStorageIsDisabled() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("test-vm")

        //VolumeInventory dataVolume = createDataVolume {
        //    name = "dataVolume"
        //    diskOfferingUuid = diskOfferingInventory.uuid
        //    primaryStorageUuid = primaryStorageSpec.inventory.uuid
        //    systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        //}

        //Map cmd1 = null
        //env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
        //    cmd1 = json(e.body, LinkedHashMap.class)
        //    return rsp
        //}
        //attachDataVolumeToVm {
        //    vmInstanceUuid = vm.uuid
        //    volumeUuid = dataVolume.uuid
        //}
        //assert cmd1 != null


        //Map cmd2 = null
        //env.afterSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
        //    cmd2 = json(e.body, LinkedHashMap.class)
        //    return rsp
        //}
        //VolumeSnapshotInventory sp = createVolumeSnapshot {
        //    volumeUuid = dataVolume.uuid
        //    name = "sp1"
        //}
        //assert cmd2 != null

        //Map cmd3 = null
        //env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
        //    cmd3 = json(e.body, LinkedHashMap.class)
        //    return rsp
        //}
        //stopVmInstance {
        //    uuid = vm.uuid
        //}
        //assert cmd3 != null
        //assert cmd3.uuid == vm.uuid
        //VmInstanceVO vmvo = dbFindByUuid(cmd3.uuid, VmInstanceVO.class)
        //assert vmvo.state == VmInstanceState.Stopped

        //changePrimaryStorageState {
        //    uuid = primaryStorageSpec.inventory.uuid
        //    stateEvent = PrimaryStorageStateEvent.disable.toString()
        //}
        //assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled


        //Map cmd4 = null
        //env.afterSimulator(LocalStorageKvmBackend.REVERT_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
        //    cmd4 = json(e.body, LinkedHashMap.class)
        //    return rsp
        //}
        //revertVolumeFromSnapshot {
        //    uuid = sp.uuid
        //}
        //assert cmd4 != null

        //changePrimaryStorageState {
        //    uuid = primaryStorageSpec.inventory.uuid
        //    stateEvent = PrimaryStorageStateEvent.enable.toString()
        //}

        //assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
