package org.zstack.test.integration.core.logs

import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.Test

class LogTest extends Test {
    @Override
    void setup() {
        useSpring(SimpleTest.springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
