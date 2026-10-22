package org.zstack.test.integration.guesttools

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class GuestToolsTest extends Test {
    static SpringSpec springSpec = makeSpring {
        include(ZStackTest.springSpec)
        include("guesttools.xml")
        include("zwatch.xml")
        include("zwatchDB.xml")
    }

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
