package org.zstack.test.integration

import org.zstack.test.unittest.JUnitTestSuite
import org.zstack.testlib.SubCase

/**
 * Created by lining on 2018/3/18.
 */
class JUnitCase extends SubCase{

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        JUnitTestSuite.runAllTestCases()
    }

    @Override
    void clean() {
    }
}
