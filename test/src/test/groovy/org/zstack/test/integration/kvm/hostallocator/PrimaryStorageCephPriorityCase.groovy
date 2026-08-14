package org.zstack.test.integration.kvm.hostallocator

import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.CephEnv
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2017/11/7.
 * if BS = CephBS, and we have 2 Ps(Ceph, LocalStorage), we should choose Ceph first
 */
class PrimaryStorageCephPriorityCase extends SubCase {
    EnvSpec env

    HostInventory host

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
        env = CephEnv.oneCephBSandtwoPs()
    }

    @Override
    void test() {
        env.create {
            testChooseCephPs()
        }
    }

    void testChooseCephPs() {
        host = env.inventoryByName("host6") as HostInventory
        def image = env.inventoryByName("image") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        def vm1 = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instanceOffering.uuid
        } as VmInstanceInventory

        assert vm1.hostUuid == host.uuid
    }
}
