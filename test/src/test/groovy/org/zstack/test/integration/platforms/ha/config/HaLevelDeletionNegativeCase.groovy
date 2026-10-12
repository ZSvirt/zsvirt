package org.zstack.test.integration.platforms.ha.config

import org.zstack.header.errorcode.SysErrors
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class HaLevelDeletionNegativeCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.normal()
    }

    @Override
    void test() {
        env.create {
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            testInvalidHaTagOnCreateVm()
        }
    }

    void testInvalidHaTagOnCreateVm() {
        logger.info("Test 001: create a VM with ha::test, expect operation error")
        expectApiFailure({
            createVmInstance {
                delegate.name = "invalid-ha-tag"
                delegate.cpuNum = 1
                delegate.memorySize = gb(1)
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.platform = "Linux"
                delegate.guestOsType = "Linux"
                delegate.architecture = "x86_64"
                delegate.diskAOs = [
                    [
                        "boot"        : true,
                        "size"        : gb(10),
                        "platform"    : "Linux",
                        "guestOsType" : "Linux",
                        "architecture": "x86_64"
                    ]
                ]
                delegate.systemTags = ["ha::test"]
            }
        }) {
            assert delegate.code == SysErrors.OPERATION_ERROR.toString()
            assert delegate.details.contains("test")
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
