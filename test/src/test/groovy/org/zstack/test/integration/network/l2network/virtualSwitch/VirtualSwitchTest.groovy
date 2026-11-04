package org.zstack.test.integration.network.l2network.virtualSwitch

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test
import org.zstack.testlib.premium.PremiumEnv

class VirtualSwitchTest extends Test {
    static SpringSpec springSpec = PremiumEnv.makeSpring()

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
