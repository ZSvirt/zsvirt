package org.zstack.test.integration.platforms.ha

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Test

class HaTest extends Test {
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
