package org.zstack.test.integration.core

import org.zstack.testlib.SubCase

/**
 * Created by lining on 2017/7/20.
 */
class RandomFailCase extends SubCase {

    static int maxRunTimes = 30

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
        maxRunTimes --

        assert maxRunTimes > 0

        Random random = new Random()
        int key = random.nextInt(maxRunTimes)

        if(key == 0){
            assert false
        } else {
            assert true
        }
    }
}
