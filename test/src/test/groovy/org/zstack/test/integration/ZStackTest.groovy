package org.zstack.test.integration

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/2/27.
 */
class ZStackTest extends Test {
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
