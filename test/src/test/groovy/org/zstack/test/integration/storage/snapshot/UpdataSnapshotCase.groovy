package org.zstack.test.integration.storage.snapshot

import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.storage.snapshot.VolumeSnapshot
import org.zstack.test.integration.ldap.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.kvm.KVMConstant
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator
import org.zstack.core.db.DatabaseFacade

/**
 * Created by ads6 on 2018/1/2.
 */
class UpdataSnapshotCase extends SubCase{

    EnvSpec env
    VmInstanceInventory vm
    VolumeSnapshotInventory root


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
            vm = env.inventoryByName("vm") as VmInstanceInventory
            testUpdateSnapshot()

        }
    }

    void testUpdateSnapshot() {
        root = createSnapshot(vm.getRootVolumeUuid())


        VolumeSnapshotInventory inv = updateVolumeSnapshot {
            delegate.uuid = root.getUuid()
            delegate.name = "test"
            delegate.description = "test-des"
        } as VolumeSnapshotInventory

        assert inv.name == "test"
        assert inv.description == "test-des"
    }


    private VolumeSnapshotInventory createSnapshot(String uuid){
        VolumeSnapshotInventory inv = createVolumeSnapshot {
            volumeUuid = uuid
            name = "test-snapshot"
        } as VolumeSnapshotInventory
        return inv
    }
}
