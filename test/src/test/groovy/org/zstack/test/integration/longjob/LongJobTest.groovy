package org.zstack.test.integration.longjob

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Test

/**
 * Created by camile on 2018/2/6.
 */
class LongJobTest extends Test {

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
