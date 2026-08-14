package org.zstack.test.integration.stabilisation

import org.junit.ClassRule
import org.zapodot.junit.ldap.EmbeddedLdapRule
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.StabilityTest

/**
 * Created by lining on 2017/7/12.
 *
 * Example:
 * mvn test -Dtest=StabilityTestCase -Dcases=org.zstack.test.integration.kvm.vm.OneVmBasicLifeCycleCase,org.zstack.test.integration.core.MustPassCase -Dtimes=2
 *
 * Q : How do I know which subcase failed ?
 * A : search keyword is 'stability test fails', example: grep "stability test fails" management-server.log
 */
class StabilityTestCase extends StabilityTest {

    public static final String DOMAIN_DSN = "dc=example,dc=com"

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build()

    @Override
    SpringSpec getDefaultSpringSpec(){
        return TestCaseStabilityTest.springSpec
    }

    @Override
    String getFailureLogsDirName() {
        return TestCaseStabilityTest.class.getName().replace(".", "_")
    }
}
