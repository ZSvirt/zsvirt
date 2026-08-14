package org.zstack.test.integration.identity

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.Test

/**
 * Created by camile on 2017/10/23.
 */
class IdentityTest extends Test {
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
