package org.zstack.test.integration.storage.volume

import org.zstack.header.volume.VolumeStatus
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by Administrator on 2017-05-19.
 */
class GetAttachableVolumeCase extends SubCase{
    EnvSpec env

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
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testGetAttachableVolume()
        }
    }

    void testGetAttachableVolume(){
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        DiskOfferingInventory disk = env.inventoryByName("diskOffering") as DiskOfferingInventory

        VolumeInventory vol = createDataVolume {
            name = "vol"
            diskOfferingUuid = disk.uuid
        }as VolumeInventory

        assert vol.status == VolumeStatus.NotInstantiated.toString()

        List<VolumeInventory> vols = getVmAttachableDataVolume {
            vmInstanceUuid = vm.uuid
        } as List<VolumeInventory>

        assert vols.size() == 1
    }
}
