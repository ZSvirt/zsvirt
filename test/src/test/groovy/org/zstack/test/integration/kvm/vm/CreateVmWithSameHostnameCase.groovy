package org.zstack.test.integration.kvm.vm

import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * @Author: fubang
 * @Date: 2018/7/9
 */
class CreateVmWithSameHostnameCase extends SubCase{
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmWithSameHostname()
        }
    }

    void testCreateVmWithSameHostname() {
        def instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def image = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        createVmInstance {
            name = "test-1"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            systemTags = ["hostname::testhost"]
        }

        /* remove the vm hostname restriction
        expect(AssertionError.class){
            createVmInstance {
                name = "test-1"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                systemTags = ["hostname::testhost"]
            }
        }*/

        List<VmInstanceInventory> vms =  queryVmInstance {
            conditions = ["type=UserVm"]
        }
        assert vms.size() == 2
    }
}
