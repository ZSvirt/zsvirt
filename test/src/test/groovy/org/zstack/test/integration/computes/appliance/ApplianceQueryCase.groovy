package org.zstack.test.integration.computes.appliance

import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ApplianceQueryCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(ApplianceTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                description = "test"
            }
        }
    }

    @Override
    void test() {
        env.create {
            testExpandQueryInVirtualRouterVm()
        }
    }

    void testExpandQueryInVirtualRouterVm() {
        logger.info("Test 001: test expand query in VirtualRouterVmVO, expect no SYS.1000")
        queryVirtualRouterVm {
            delegate.conditions = [
                "virtualRouterOffering.uuid=00000000000000000000000000000000"
            ]
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
