package org.zstack.test.integration.stabilisation

import org.zstack.testlib.AbstractTestCaseStabilityTest
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Example:
 * mvn test -Dtest=TestCaseStabilityTest -Dcases=org.zstack.test.integration.zone.ZoneCase -Dtimes=3
 *
 * Repeat a designated case:
 * mvn test -Dtest=StabilityTestCase -Dcases=org.zstack.test.integration.zone.ZoneCase -Dtimes=3
 */
class TestCaseStabilityTest extends AbstractTestCaseStabilityTest {
    static SpringSpec springSpec = Test.makeSpring()

    @Override
    SpringSpec getSpringSpec() {
        return springSpec
    }
}
