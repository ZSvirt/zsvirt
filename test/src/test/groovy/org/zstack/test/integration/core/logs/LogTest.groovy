package org.zstack.test.integration.core.logs

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Test

class LogTest extends Test {
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
