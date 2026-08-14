package org.zstack.test.integration.kvm.vm

import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2017/9/20.
 */
class CreateInstanceOfferingCase extends SubCase {
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
        env = Env.noVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateInstanceOfferingMemorySizeCheck()
        }
    }

    void testCreateInstanceOfferingMemorySizeCheck() {
        try {
            createInstanceOffering {
                name = "test"
                cpuNum = 1
                memorySize = Long.MAX_VALUE + 1
            }
        } catch (Exception e) {
            assert e.message.contains("bytes")
        }
    }
}
