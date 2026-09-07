package org.zstack.test.integration.stabilisation

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

    @Override
    SpringSpec getDefaultSpringSpec(){
        return TestCaseStabilityTest.springSpec
    }

    @Override
    String getFailureLogsDirName() {
        return TestCaseStabilityTest.class.getName().replace(".", "_")
    }
}
