package org.zstack.test.integration.stabilisation

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.AbstractTestCaseStabilityTest
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.premium.PremiumEnv

/**
 * Example:
 * mvn test -Dtest=TestCaseStabilityTest -Dcases=org.zstack.test.integration.zone.ZoneCase -Dtimes=3
 *
 * Repeat a designated case:
 * mvn test -Dtest=StabilityTestCase -Dcases=org.zstack.test.integration.zone.ZoneCase -Dtimes=3
 */
class TestCaseStabilityTest extends AbstractTestCaseStabilityTest {
    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    static SpringSpec springSpec = PremiumEnv.makeSpring()

    @Override
    SpringSpec getSpringSpec() {
        return springSpec
    }
}
