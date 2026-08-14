package org.zstack.test.integration.identity

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/5/17.
 */
class IsOpensourceCase extends SubCase {
    @Override
    void clean() {

    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        assert isOpensourceVersion {}.opensource
    }
}
