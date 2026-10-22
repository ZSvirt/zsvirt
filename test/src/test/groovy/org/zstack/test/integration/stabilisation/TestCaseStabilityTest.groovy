package org.zstack.test.integration.stabilisation

import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.AbstractTestCaseStabilityTest
import org.zstack.testlib.SpringSpec

/**
 * Created by lining on 2017/7/15.
 *
 * Example:
 * mvn test -Dtest=TestCaseStabilityTest -Dcases=org.zstack.test.integration.core.MustPassCase -Dtimes=3
 *
 * Q : How do I know which subcase failed ?
 * A : search keyword is 'stability test fails', likes: grep "stability test fails" management-server.log
 */
class TestCaseStabilityTest extends AbstractTestCaseStabilityTest {

    static SpringSpec springSpec = makeSpring {
        include(ZStackTest.springSpec)
        include("guesttools.xml")
        include("zwatch.xml")
        include("zwatchDB.xml")
    }

    @Override
    SpringSpec getSpringSpec() {
        return springSpec
    }
}
