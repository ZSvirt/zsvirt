package org.zstack.test.integration.kvm.host

import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.sdk.HostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kefeng.wang on 2019/1/7.*/
class UpdateHostCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.twoHostEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testUpdateHost()
        }
    }

    void testUpdateHost() {
        HostInventory kvm = env.inventoryByName("kvm1")
        assert kvm.name == "kvm1"
        assert kvm.description == null
        assert kvm.managementIp == "127.0.0.2"

        expect([ApiMessageInterceptionException.class, AssertionError.class]) {
            updateHost {
                uuid = kvm.uuid
                managementIp = "127.0.0.3"
            }
        }

        updateHost {
            uuid = kvm.uuid
            name = "kvm-name"
            description = "kvm-description"
            managementIp = "127.0.0.4"
        }

        List<HostInventory> kvms = queryHost {
            conditions = ["uuid=${kvm.uuid}"]
        }
        assert kvms.size() == 1

        HostInventory kvm2 = kvms.get(0)
        assert kvm2.name == "kvm-name"
        assert kvm2.description == "kvm-description"
        assert kvm2.managementIp == "127.0.0.4"
    }
}
