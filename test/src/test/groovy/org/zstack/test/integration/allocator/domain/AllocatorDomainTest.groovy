package org.zstack.test.integration.allocator.domain

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test
import org.zstack.testlib.premium.PremiumEnv

class AllocatorDomainTest extends Test {
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
