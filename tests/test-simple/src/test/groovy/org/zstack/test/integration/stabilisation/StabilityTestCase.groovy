package org.zstack.test.integration.stabilisation

import org.zstack.testlib.SkipTestSuite
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.StabilityTest

/**
 * Example:
 * mvn test -Dtest=StabilityTestCase -Dcases=org.zstack.test.integration.zone.ZoneCase -Dtimes=3
 *
 * Q : How do I know which subcase failed ?
 * A : search keyword is 'stability test fails', example: grep "stability test fails" management-server.log
 */
@SkipTestSuite
class StabilityTestCase extends StabilityTest {
    @Override
    SpringSpec getDefaultSpringSpec() {
        return TestCaseStabilityTest.springSpec
    }

    @Override
    String getFailureLogsDirName() {
        return TestCaseStabilityTest.class.getName().replace(".", "_")
    }
}
