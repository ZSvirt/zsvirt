package org.zstack.test.integration.storage.primary.nfs

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-05-31.
 */
class NfsAttachVolumeMaintainCase extends SubCase{
    EnvSpec env
    HostInventory host1
    PrimaryStorageInventory nfs1
    VmInstanceInventory vm
    VolumeInventory disk
    DiskOfferingInventory diskOff

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
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm") as VmInstanceInventory
            nfs1 = env.inventoryByName("nfs") as PrimaryStorageInventory
            host1 = env.inventoryByName("kvm") as HostInventory
            diskOff = env.inventoryByName("diskOffering") as DiskOfferingInventory
            testCreateVolumeWhenNfs(PrimaryStorageStateEvent.enable, true)
            testAttachVolumeWhenNfs(PrimaryStorageStateEvent.maintain, disk, vm.uuid, false)
        }
    }

    void testCreateVolumeWhenNfs(PrimaryStorageStateEvent state, boolean Expect){
        changePrimaryStorageState {
            uuid = nfs1.uuid
            stateEvent = state
        }

        CreateDataVolumeAction a = new CreateDataVolumeAction()

        a.name = "data"
        a.diskOfferingUuid = diskOff.uuid
        a.primaryStorageUuid = nfs1.uuid
        a.sessionId = currentEnvSpec.session.uuid

        def ret = a.call()
        if(Expect){
            assert ret.error == null
            disk = ret.value.inventory
        }else {
            assert ret.error != null
        }
    }

    void testAttachVolumeWhenNfs(PrimaryStorageStateEvent state, VolumeInventory disk, String vmUuid, boolean Expect){
        changePrimaryStorageState {
            uuid = disk.primaryStorageUuid
            stateEvent = state
        }

        AttachDataVolumeToVmAction a = new AttachDataVolumeToVmAction()

        a.vmInstanceUuid = vmUuid
        a.volumeUuid = disk.uuid
        a.sessionId = currentEnvSpec.session.uuid

        if(Expect){
            assert a.call().error == null
        }else {
            assert a.call().error != null
        }
    }
}
