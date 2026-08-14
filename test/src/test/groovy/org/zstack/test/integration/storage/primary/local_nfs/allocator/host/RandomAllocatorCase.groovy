package org.zstack.test.integration.storage.primary.local_nfs.allocator.host

import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by mingjian.deng on 2017/12/22.
 */
class RandomAllocatorCase extends SubCase {
    EnvSpec env

    ImageInventory image
    L3NetworkInventory l3
    InstanceOfferingInventory offering

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
        env = LocalAllocatorEnv.tenHosts()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCreateVmRandom()
        }
    }

    void prepare() {
        image = env.inventoryByName("image") as ImageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
    }

    void testCreateVmRandom() {
        HashSet<String> hosts = new HashSet<>()
        int i = 0
        while (i++ < 10) {
            def vm = createVmInstance {
                name = "vm"
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = offering.uuid
            } as VmInstanceInventory
            hosts.add(vm.hostUuid)
            logger.debug("only for test:" + vm.hostUuid)
        }
        assert hosts.size() > 1
    }
}
