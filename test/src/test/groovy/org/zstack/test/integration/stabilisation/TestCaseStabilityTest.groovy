package org.zstack.test.integration.stabilisation

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
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

    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    static SpringSpec springSpec = ZStackTest.springSpec

    @Override
    SpringSpec getSpringSpec() {
        return springSpec
    }
}
