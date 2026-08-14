package org.zstack.test.integration.core

import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/4/10.
 */
class MustFailCase extends SubCase {

    @Override
    void clean() {
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        // context : Verify the validity of the test results, and when the case fails, the test results that are expected to generate the test framework are also failures
        // must fail
        assert false
    }
}
