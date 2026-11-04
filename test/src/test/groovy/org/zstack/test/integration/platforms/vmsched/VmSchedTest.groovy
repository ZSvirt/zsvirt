package org.zstack.test.integration.platforms.vmsched

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Test

class VmSchedTest extends Test {
    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
