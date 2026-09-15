package org.zstack.test.integration

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class SimpleTest extends Test {
    static SpringSpec springSpec = makeSpring()

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
