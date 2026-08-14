package org.zstack.test.integration.storage.primary.local.psmaintenance

import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

/**
 * Created by HeathHose on 2017/3/7.
 */
class LocalStorageMaintenanceVolumNotInstantiatedCase extends SubCase{
    def DOC = """

    GetDataVolumeAttachableVm don't list the vm which ps is maintain

"""
    EnvSpec env
    private  final static CLogger logger = Utils.getLogger(LocalStorageMaintenanceVolumNotInstantiatedCase.class);
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create{
            testListAvailableVmWhenPrimaryStorageisMaintenance()
            testListAvailableVmWhenPrimaryStorageisOtherState()
        }

    }

    void testListAvailableVmWhenPrimaryStorageisMaintenance(){
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory primaryStorage = env.inventoryByName("local")

        VolumeInventory volumeInventory = createDataVolume {
            name = "volume1"
            diskOfferingUuid = diskOffering.uuid
        }
        primaryStorage = changePrimaryStorageState {
            uuid=primaryStorage.uuid
            stateEvent = "maintain"
        }
        List<VmInstanceInventory> list = getDataVolumeAttachableVm {
            volumeUuid = volumeInventory.uuid
        }
        assert list.isEmpty() == true
    }

    void testListAvailableVmWhenPrimaryStorageisOtherState(){
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        PrimaryStorageInventory primaryStorage = env.inventoryByName("local")

        VolumeInventory volumeInventory = createDataVolume {
            name = "volume2"
            diskOfferingUuid = diskOffering.uuid
        }

        primaryStorage = changePrimaryStorageState {
            uuid=primaryStorage.uuid
            stateEvent = "enable"
        }

        retryInSecs{
            List<VmInstanceInventory> list = getDataVolumeAttachableVm {
                volumeUuid = volumeInventory.uuid
            }
            assert list.isEmpty() == false
        }

        primaryStorage = changePrimaryStorageState {
            uuid=primaryStorage.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        retryInSecs{
            List<VmInstanceInventory> list = getDataVolumeAttachableVm {
                volumeUuid = volumeInventory.uuid
            }
            assert list.isEmpty() == true
        }

        primaryStorage = changePrimaryStorageState {
            uuid=primaryStorage.uuid
            stateEvent = "deleting"
        }

        retryInSecs{
            List<VmInstanceInventory> list = getDataVolumeAttachableVm {
                volumeUuid = volumeInventory.uuid
            }
            assert list.isEmpty() == true
        }
    }



    @Override
    void clean() {
        env.delete()
    }
}
