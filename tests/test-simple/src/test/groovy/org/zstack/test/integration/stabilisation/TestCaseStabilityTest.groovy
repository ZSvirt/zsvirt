package org.zstack.test.integration.stabilisation

import org.zstack.test.integration.identity.IdentityTest
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

    static {
        // The suite builds Spring before running the stability subcases.
        springSpec.include("LogConfiguration.xml")
        springSpec.include("Log4j2Configuration.xml")
        // AccountCase needs its authentication extensions before the shared context starts.
        if (System.getProperty("cases", "").split(",").any {
            it.trim() == "org.zstack.test.integration.identity.account.AccountCase"
        }) {
            springSpec.xmls.addAll(IdentityTest.springSpec.xmls)
        }
    }

    @Override
    SpringSpec getSpringSpec() {
        return springSpec
    }
}
