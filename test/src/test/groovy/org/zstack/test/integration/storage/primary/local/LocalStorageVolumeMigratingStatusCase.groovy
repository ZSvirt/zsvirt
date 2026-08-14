package org.zstack.test.integration.storage.primary.local

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.volume.ChangeVolumeStatusMsg
import org.zstack.header.volume.ChangeVolumeStatusReply
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by kayo on 2018/3/12.
 */
class LocalStorageVolumeMigratingStatusCase extends SubCase {
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

        }
    }

    void testVolumeMigratingStatus() {
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        KVMHostInventory host = env.inventoryByName("kvm1") as KVMHostInventory

        VolumeInventory volume = createDataVolume {
            name = "d"
            diskOfferingUuid = diskOffering.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = volume.uuid
        }

        detachDataVolumeFromVm {
            vmUuid = vm.uuid
            uuid = volume.uuid
        }

        def ret = new ConcurrentLinkedQueue()
        env.message(ChangeVolumeStatusMsg.class) { ChangeVolumeStatusMsg msg, CloudBus bus ->
            ret.add(msg.getStatus())
            def reply = new ChangeVolumeStatusReply()
            bus.reply(msg, reply)
        }

        localStorageMigrateVolume {
            volumeUuid = volume.uuid
            destHostUuid = host.uuid
        }
        
        assert ret.contains("Migrating")
        assert ret.contains("Ready")
    }

}
